package com.kh.magamGG.global.security;

import com.kh.magamGG.domain.auth.service.RefreshTokenService;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 구글 로그인 성공 핸들러
 * 
 * 주요 기능:
 * 1. 구글 로그인 성공 시 Access Token과 Refresh Token 발급
 * 2. Access Token: Response Header 또는 JSON 바디로 전달
 * 3. Refresh Token: HttpOnly, Secure, SameSite=Strict 쿠키에 저장 + Valkey에 저장
 * 4. Local 환경에서는 쿠키의 secure=false 처리
 * 
 * 보안 고려사항:
 * - Refresh Token은 쿠키에만 저장 (JavaScript 접근 불가)
 * - SameSite=Strict로 CSRF 공격 방지
 * - Valkey에 토큰 해시 저장 (평문 저장 방지)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * 구글 로그인 성공 시 호출되는 메서드
     * 
     * 처리 흐름:
     * 1. OAuth2User에서 이메일 추출
     * 2. DB에서 회원 조회 (없으면 예외 발생)
     * 3. Access Token 발급
     * 4. Refresh Token 발급
     * 5. Refresh Token을 쿠키에 저장 + Valkey에 저장
     * 6. 프론트엔드로 리다이렉트 (Access Token을 쿼리 파라미터로 전달)
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("🔐 [OAuth2] 구글 로그인 성공 처리 시작");

        // 1. OAuth2User에서 이메일 추출
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        
        if (email == null || email.isEmpty()) {
            log.error("❌ [OAuth2] 이메일을 가져올 수 없음");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "이메일을 가져올 수 없습니다.");
            return;
        }

        log.info("✅ [OAuth2] 이메일 추출 완료: {}", email);

        // 2. DB에서 회원 조회
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ [OAuth2] 회원을 찾을 수 없음: {}", email);
                    return new MemberNotFoundException("구글 계정으로 가입된 회원을 찾을 수 없습니다.");
                });

        // 3. Access Token 발급
        log.info("🎫 [OAuth2] Access Token 발급 시작: memberNo={}, email={}", member.getMemberNo(), email);
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getMemberNo(),
                member.getMemberEmail()
        );
        log.info("✅ [OAuth2] Access Token 발급 완료: memberNo={}, tokenLength={}", 
                member.getMemberNo(), accessToken != null ? accessToken.length() : 0);

        // 4. Refresh Token 발급
        log.info("🎫 [OAuth2] Refresh Token 발급 시작: memberNo={}", member.getMemberNo());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getMemberNo());
        log.info("✅ [OAuth2] Refresh Token 발급 완료: memberNo={}, tokenLength={}", 
                member.getMemberNo(), refreshToken != null ? refreshToken.length() : 0);

        // 5. Refresh Token을 Valkey에 저장
        log.info("💾 [OAuth2] Valkey에 Refresh Token 저장 시작: email={}", email);
        try {
            refreshTokenService.saveRefreshToken(member.getMemberEmail(), refreshToken);
            
            // 저장 확인 (검증)
            String savedTokenHash = refreshTokenService.getRefreshToken(member.getMemberEmail());
            if (savedTokenHash != null) {
                log.info("✅ [OAuth2] Valkey 저장 확인 완료: email={}, key=RT:{}", 
                        email, email);
            } else {
                log.error("❌ [OAuth2] Valkey 저장 실패: email={} (저장 후 조회 시 null)", email);
                throw new RuntimeException("Refresh Token 저장 실패: Valkey에 저장되지 않았습니다.");
            }
        } catch (Exception e) {
            log.error("❌ [OAuth2] Valkey 저장 중 예외 발생: email={}, error={}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("Refresh Token 저장 실패", e);
        }

        // 6. Refresh Token을 쿠키에 저장
        addRefreshTokenCookie(response, refreshToken);

        log.info("✅ [OAuth2] 토큰 발급 및 저장 완료: memberNo={}, email={}", member.getMemberNo(), email);

        // 7. 프론트엔드로 리다이렉트 (Access Token을 쿼리 파라미터로 전달)
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth2/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("memberNo", member.getMemberNo())
                .queryParam("memberName", URLEncoder.encode(member.getMemberName(), StandardCharsets.UTF_8))
                .queryParam("memberRole", member.getMemberRole())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Refresh Token을 HttpOnly 쿠키에 저장
     * 
     * 쿠키 설정:
     * - HttpOnly: true (JavaScript 접근 불가, XSS 공격 방지)
     * - Secure: true (HTTPS에서만 전송, Local 환경에서는 false)
     * - SameSite: Strict (CSRF 공격 방지)
     * - Path: / (모든 경로에서 사용 가능)
     * - MaxAge: jwt.refresh-expiration (초 단위)
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // MaxAge를 초 단위로 변환 (밀리초 → 초)
        int maxAge = (int) (refreshExpiration / 1000);
        
        // SameSite 설정
        boolean isLocal = "local".equals(activeProfile);
        String secureFlag = isLocal ? "" : "Secure; ";
        
        String cookieValue = String.format(
            "refreshToken=%s; HttpOnly; Path=/; Max-Age=%d; %sSameSite=Strict",
            refreshToken,
            maxAge,
            secureFlag
        );
        
        response.addHeader("Set-Cookie", cookieValue);

        log.debug("🍪 Refresh Token 쿠키 설정 완료: HttpOnly=true, Secure={}, MaxAge={}초",
                !isLocal, maxAge);
    }
}

