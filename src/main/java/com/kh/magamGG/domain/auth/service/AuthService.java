package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.request.RefreshTokenRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.dto.response.RefreshTokenResponse;
import com.kh.magamGG.domain.auth.entity.RefreshToken;
import com.kh.magamGG.domain.auth.repository.RefreshTokenRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.*;
import com.kh.magamGG.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인 처리
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 회원 조회
        Member member = memberRepository.findByMemberEmail(request.getMemberEmail())
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 이메일입니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getMemberPassword(), member.getMemberPassword())) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (!"ACTIVE".equals(member.getMemberStatus()) && !"활성".equals(member.getMemberStatus())) {
            throw new InvalidCredentialsException("비활성화된 계정입니다.");
        }

        // Access Token 발급
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(), 
                member.getMemberEmail()
        );

        // Refresh Token 발급
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());

        // Refresh Token 해시 생성
        String tokenHash = jwtTokenProvider.hashToken(refreshToken);

        // Token Family UUID 생성
        String tokenFamily = UUID.randomUUID().toString();

        // 만료 시간 계산 (application.yaml의 refreshExpiration 값 사용)
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpiration() / 1000);

        // Refresh Token DB 저장 (해시만 저장)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .memberNo(member.getMemberNo())
                .refreshTokenHash(tokenHash)
                .refreshTokenFamily(tokenFamily)
                .refreshTokenIsRevoked("F")
                .refreshTokenExpiresAt(expiryDate)
                .refreshTokenCreatedAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("로그인 성공: {} ({})", member.getMemberName(), member.getMemberEmail());

        // Agency 번호 추출
        Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;

        return LoginResponse.builder()
                .token(accessToken)  // 기존 프론트엔드와 호환성 유지
                .accessToken(accessToken)  // 새 필드 추가
                .refreshToken(refreshToken)
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberRole(member.getMemberRole())
                .agencyNo(agencyNo)
                .memberProfileImage(member.getMemberProfileImage())  // 프로필 이미지 추가
                .build();
    }

    /**
     * Refresh Token으로 Access Token 갱신
     * Token Rotation 방식 적용
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("🔄 [토큰 갱신] refreshToken() 메서드 호출 시작");
        String refreshTokenValue = request.getRefreshToken();

        // 1. Refresh 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenValue)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. 토큰 해시 생성
        String tokenHash = jwtTokenProvider.hashToken(refreshTokenValue);
        log.debug("🔄 [토큰 갱신] 토큰 해시 생성 완료: {}", tokenHash.substring(0, 16) + "...");

        // 3. DB 조회 (활성 토큰만 조회)
        log.debug("🔄 [토큰 갱신] DB에서 활성 토큰 조회 시작");
        RefreshToken refreshToken = refreshTokenRepository
                .findActiveByRefreshTokenHash(tokenHash)
                .orElse(null);

        // 3-1. revoked된 토큰 재사용 감지
        if (refreshToken == null) {
            // revoked된 토큰인지 확인
            Optional<RefreshToken> revokedTokenOpt = refreshTokenRepository
                    .findRevokedByRefreshTokenHash(tokenHash);
            
            if (revokedTokenOpt.isPresent()) {
                // revoked된 토큰이 다시 사용됨 → token_family 전체 revoke
                RefreshToken revokedToken = revokedTokenOpt.get();
                List<RefreshToken> familyTokens = refreshTokenRepository
                        .findByRefreshTokenFamily(revokedToken.getRefreshTokenFamily());
                
                familyTokens.forEach(RefreshToken::revoke);
                refreshTokenRepository.saveAll(familyTokens);
                refreshTokenRepository.flush(); // 즉시 DB 반영 (재사용 공격 방어)
                
                // 보안 로그: 재사용 공격 감지
                log.error("🔒 [보안 경고] revoked된 토큰 재사용 감지: tokenFamily={}, memberNo={}, IP={}", 
                        revokedToken.getRefreshTokenFamily(), revokedToken.getMemberNo(), 
                        "IP 추적 필요"); // TODO: HttpServletRequest에서 IP 추출
                throw new TokenReuseDetectedException("이미 사용된 토큰입니다. 모든 세션이 차단되었습니다.");
            }
            
            // 토큰이 DB에 아예 없음 (재사용 공격 가능성)
            Long memberNo = jwtTokenProvider.getMemberIdFromRefreshToken(refreshTokenValue);
            handleTokenReuse(memberNo, refreshTokenValue);
            throw new TokenNotFoundException("Refresh Token을 찾을 수 없습니다.");
        }

        // 6. expiryDate 지났으면 ExpiredTokenException
        if (refreshToken.isExpired()) {
            throw new ExpiredTokenException("만료된 Refresh Token입니다.");
        }

        // 7. 재사용 감지 로직
        List<RefreshToken> familyTokens = refreshTokenRepository
                .findByRefreshTokenFamily(refreshToken.getRefreshTokenFamily());

        // 현재 토큰을 제외한 다른 토큰이 활성 상태면 재사용 공격
        boolean reuseDetected = familyTokens.stream()
                .filter(token -> !token.getRefreshTokenId().equals(refreshToken.getRefreshTokenId()))
                .anyMatch(token -> !token.isRevoked() && !token.isExpired());

        if (reuseDetected) {
            // 같은 tokenFamily 전부 revoked 처리
            familyTokens.forEach(RefreshToken::revoke);
            refreshTokenRepository.saveAll(familyTokens);
            refreshTokenRepository.flush(); // 즉시 DB 반영 (재사용 공격 방어)

            // 보안 로그: 재사용 공격 감지
            log.error("🔒 [보안 경고] 토큰 재사용 공격 감지: tokenFamily={}, memberNo={}, IP={}", 
                    refreshToken.getRefreshTokenFamily(), refreshToken.getMemberNo(),
                    "IP 추적 필요"); // TODO: HttpServletRequest에서 IP 추출
            throw new TokenReuseDetectedException("토큰 재사용이 감지되었습니다. 모든 세션이 차단되었습니다.");
        }

        // 8. 기존 refresh revoked 처리 (중요!)
        log.debug("🔄 [토큰 갱신] 기존 토큰 revoked 처리: tokenFamily={}", refreshToken.getRefreshTokenFamily());
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // 9. 새로운 Access Token 발급
        Long memberNo = refreshToken.getMemberNo();
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        log.debug("🔄 [토큰 갱신] 새 Access Token 발급 시작: memberNo={}", memberNo);
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(),
                member.getMemberEmail()
        );

        // 10. 새로운 Refresh Token 발급
        log.debug("🔄 [토큰 갱신] 새 Refresh Token 발급 시작");
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());
        String newTokenHash = jwtTokenProvider.hashToken(newRefreshToken);

        // 11. 새로운 Refresh Token DB 저장 (같은 tokenFamily 유지)
        log.debug("🔄 [토큰 갱신] 새 Refresh Token DB 저장: tokenFamily={}", refreshToken.getRefreshTokenFamily());
        
        // 🔒 동시성 문제 방지: 새 토큰 해시가 이미 존재하는지 확인
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository
                .findByRefreshTokenHashAndRefreshTokenIsRevoked(newTokenHash, "F");
        if (existingTokenOpt.isPresent()) {
            // 이미 존재하는 토큰 (동시 요청으로 인한 중복)
            log.warn("⚠️ [토큰 갱신] 새 토큰 해시가 이미 존재함 (동시 요청 감지): tokenHash={}, tokenFamily={}", 
                    newTokenHash.substring(0, 16) + "...", existingTokenOpt.get().getRefreshTokenFamily());
            // 기존 토큰이 이미 있으므로 정상 응답 반환
        } else {
            // 새 토큰 저장
            RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                    .memberNo(member.getMemberNo())
                    .refreshTokenHash(newTokenHash)
                    .refreshTokenFamily(refreshToken.getRefreshTokenFamily()) // 같은 패밀리 유지
                    .refreshTokenIsRevoked("F")
                    .refreshTokenExpiresAt(LocalDateTime.now()
                            .plusSeconds(jwtTokenProvider.getRefreshExpiration() / 1000))
                    .refreshTokenCreatedAt(LocalDateTime.now())
                    .build();

            try {
                refreshTokenRepository.save(newRefreshTokenEntity);
                refreshTokenRepository.flush(); // 즉시 DB 반영
                log.debug("🔄 [토큰 갱신] 새 토큰 저장 완료");
            } catch (DataIntegrityViolationException e) {
                // UNIQUE 제약 위반 (동시 요청으로 인한 중복 저장)
                log.warn("⚠️ [토큰 갱신] UNIQUE 제약 위반 감지 (동시 요청): tokenHash={}, error={}", 
                        newTokenHash.substring(0, 16) + "...", e.getMessage());
                // 이미 저장된 것으로 간주하고 정상 응답 반환
                // (다른 요청이 이미 저장했으므로)
            }
        }

        log.info("✅ [토큰 갱신] 성공: memberNo={}, tokenFamily={}", memberNo, refreshToken.getRefreshTokenFamily());

        // 12. 응답 반환
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        // Refresh Token 해시 생성
        String tokenHash = jwtTokenProvider.hashToken(refreshTokenValue);

        // 해당 토큰 조회 (활성 토큰만)
        RefreshToken refreshToken = refreshTokenRepository
                .findActiveByRefreshTokenHash(tokenHash)
                .orElse(null);

        if (refreshToken != null) {
            // 해당 토큰 revoked=true 처리
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
            log.info("로그아웃 처리: memberNo={}", refreshToken.getMemberNo());
        }
    }

    /**
     * 토큰 재사용 공격 처리
     */
    private void handleTokenReuse(Long memberNo, String tokenValue) {
        // 보안 로그: 토큰 재사용 의심
        log.error("🔒 [보안 경고] 토큰 재사용 의심: memberNo={}, tokenHash={}", 
                memberNo, jwtTokenProvider.hashToken(tokenValue).substring(0, 16) + "...");
        // 필요시 추가 보안 조치 (예: 회원 알림, 관리자 알림 등)
    }
}
