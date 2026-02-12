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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

        // 만료 시간 계산 (7일)
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

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
                .build();
    }

    /**
     * Refresh Token으로 Access Token 갱신
     * Token Rotation 방식 적용
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        // 1. Refresh 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenValue)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. 토큰 해시 생성
        String tokenHash = jwtTokenProvider.hashToken(refreshTokenValue);

        // 3. DB 조회 (revoked가 "F"인 것만 조회)
        RefreshToken refreshToken = refreshTokenRepository
                .findByRefreshTokenHashAndRefreshTokenIsRevoked(tokenHash, "F")
                .orElse(null);

        // 4. 없으면 TokenNotFoundException
        if (refreshToken == null) {
            // 재사용 공격 가능성: 토큰은 유효하지만 DB에 없음
            Long memberNo = jwtTokenProvider.getMemberIdFromRefreshToken(refreshTokenValue);
            handleTokenReuse(memberNo, refreshTokenValue);
            throw new TokenNotFoundException("Refresh Token을 찾을 수 없습니다.");
        }

        // 5. revoked=true면 RevokedTokenException
        if (refreshToken.isRevoked()) {
            throw new RevokedTokenException("무효화된 Refresh Token입니다.");
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

            log.warn("재사용 공격 감지: tokenFamily {} 무효화", refreshToken.getRefreshTokenFamily());
            throw new TokenReuseDetectedException("토큰 재사용이 감지되었습니다. 모든 세션이 차단되었습니다.");
        }

        // 8. 기존 refresh revoked 처리 (중요!)
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // 9. 새로운 Access Token 발급
        Long memberNo = refreshToken.getMemberNo();
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(),
                member.getMemberEmail()
        );

        // 10. 새로운 Refresh Token 발급
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());
        String newTokenHash = jwtTokenProvider.hashToken(newRefreshToken);

        // 11. 새로운 Refresh Token DB 저장 (같은 tokenFamily 유지)
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .memberNo(member.getMemberNo())
                .refreshTokenHash(newTokenHash)
                .refreshTokenFamily(refreshToken.getRefreshTokenFamily()) // 같은 패밀리 유지
                .refreshTokenIsRevoked("F")
                .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
                .refreshTokenCreatedAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("토큰 갱신 성공: memberNo={}", memberNo);

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

        // 해당 토큰 조회 (revoked가 "F"인 것만)
        RefreshToken refreshToken = refreshTokenRepository
                .findByRefreshTokenHashAndRefreshTokenIsRevoked(tokenHash, "F")
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
        log.warn("토큰 재사용 의심: memberNo={}", memberNo);
        // 필요시 추가 보안 조치 (예: 회원 알림, 관리자 알림 등)
    }
}
