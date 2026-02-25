package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.InvalidCredentialsException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
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
     * 회원번호로 이메일 조회 (토큰 재발급 시 사용)
     */
    public String getMemberEmail(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        return member.getMemberEmail();
    }
}
