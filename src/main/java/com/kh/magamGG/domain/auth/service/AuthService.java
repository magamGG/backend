package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.request.RefreshTokenRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.dto.response.RefreshTokenResponse;
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

/**
 * 인증 서비스 (Valkey 기반)
 * 
 * 주요 기능:
 * - RefreshTokenService (Valkey)를 사용하여 Refresh Token 관리
 * - Valkey의 TTL 기능으로 자동 만료 처리
 * - Refresh Token Rotation 방식 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;  // Valkey 기반
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인 처리 (Valkey 기반)
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

        log.info("🔐 [로그인] 회원 검증 완료: memberNo={}, email={}", member.getMemberNo(), member.getMemberEmail());

        // Access Token 발급
        log.info("🎫 [로그인] Access Token 발급 시작: memberNo={}", member.getMemberNo());
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(), 
                member.getMemberEmail()
        );
        log.info("✅ [로그인] Access Token 발급 완료: memberNo={}, tokenLength={}", 
                member.getMemberNo(), accessToken != null ? accessToken.length() : 0);

        // Refresh Token 발급
        log.info("🎫 [로그인] Refresh Token 발급 시작: memberNo={}", member.getMemberNo());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());
        log.info("✅ [로그인] Refresh Token 발급 완료: memberNo={}, tokenLength={}", 
                member.getMemberNo(), refreshToken != null ? refreshToken.length() : 0);

        // Refresh Token을 Valkey에 저장 (이메일을 키로 사용)
        log.info("💾 [로그인] Valkey에 Refresh Token 저장 시작: email={}", member.getMemberEmail());
        try {
            refreshTokenService.saveRefreshToken(member.getMemberEmail(), refreshToken);
            
            // 저장 확인 (검증)
            String savedTokenHash = refreshTokenService.getRefreshToken(member.getMemberEmail());
            if (savedTokenHash != null) {
                log.info("✅ [로그인] Valkey 저장 확인 완료: email={}, key=RT:{}", 
                        member.getMemberEmail(), member.getMemberEmail());
            } else {
                log.error("❌ [로그인] Valkey 저장 실패: email={} (저장 후 조회 시 null)", member.getMemberEmail());
                throw new RuntimeException("Refresh Token 저장 실패: Valkey에 저장되지 않았습니다.");
            }
        } catch (Exception e) {
            log.error("❌ [로그인] Valkey 저장 중 예외 발생: email={}, error={}", 
                    member.getMemberEmail(), e.getMessage(), e);
            throw new RuntimeException("Refresh Token 저장 실패", e);
        }

        log.info("✅ [로그인] 로그인 성공 (Valkey): {} ({}), memberNo={}", 
                member.getMemberName(), member.getMemberEmail(), member.getMemberNo());
        
        // 사용자 로그인 성공 로그 (콘솔 출력용)
        log.info("사용자 {} 로그인 성공 - 시각: {}", member.getMemberEmail(), LocalDateTime.now());

        // Agency 번호 추출
        Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;

        return LoginResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberRole(member.getMemberRole())
                .agencyNo(agencyNo)
                .build();
    }

    /**
     * Refresh Token으로 Access Token 갱신 (Valkey 기반)
     * Token Rotation 방식 적용
     */
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("🔄 [토큰 갱신] refreshToken() 메서드 호출 시작 (Valkey)");
        String refreshTokenValue = request.getRefreshToken();

        // 1. Refresh 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenValue)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token에서 회원번호 추출
        Long memberNo = jwtTokenProvider.getMemberIdFromRefreshToken(refreshTokenValue);
        
        // 3. 회원 정보 조회 (이메일 가져오기)
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        
        String email = member.getMemberEmail();

        // 4. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(),
                member.getMemberEmail()
        );

        // 5. 새 Refresh Token 발급
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());

        // 6. Refresh Token Rotation (Valkey 기반)
        // validateAndRotate: 기존 토큰 검증 → 삭제 → 새 토큰 저장 (원자적 연산)
        try {
            refreshTokenService.validateAndRotate(email, refreshTokenValue, newRefreshToken);
        } catch (InvalidTokenException e) {
            // 토큰 불일치 또는 없음 → 탈취된 토큰으로 간주
            log.error("🔒 [보안 경고] Refresh Token 검증 실패: email={}", email);
            throw e;
        }

        log.info("✅ [토큰 갱신] 성공 (Valkey): email={}", email);

        // 7. 응답 반환
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 로그아웃 처리 (Valkey 기반)
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        // Refresh Token에서 회원번호 추출
        Long memberNo = jwtTokenProvider.getMemberIdFromRefreshToken(refreshTokenValue);
        
        // 회원 정보 조회 (이메일 가져오기)
        Member member = memberRepository.findById(memberNo)
                .orElse(null);
        
        if (member != null) {
            // Valkey에서 Refresh Token 삭제
            refreshTokenService.deleteRefreshToken(member.getMemberEmail());
            log.info("✅ 로그아웃 처리 (Valkey): email={}", member.getMemberEmail());
        }
    }

    /**
     * 회원번호로 이메일 조회 (토큰 재발급 시 사용)
     */
    public String getMemberEmail(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        return member.getMemberEmail();
    }
}
