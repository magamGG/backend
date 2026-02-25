package com.kh.magamGG.domain.auth.controller;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.service.AuthService;
import com.kh.magamGG.domain.auth.service.GoogleOAuthService;
import com.kh.magamGG.global.exception.OAuthRegistrationRequiredException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        
        // Refresh Token을 쿠키에 저장 (reissue 시 쿠키에서 읽기 위해)
        if (response.getRefreshToken() != null) {
            addRefreshTokenCookie(httpResponse, response.getRefreshToken());
            log.info("✅ [로그인] Refresh Token 쿠키 저장 완료");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * OAuth 콜백 처리 (범용)
     * 지원: google, naver, kakao
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) {
        try {
            // 사용자가 취소한 경우 또는 에러가 있는 경우
            if (error != null || code == null || code.isEmpty()) {
                log.warn("{} OAuth 로그인 취소 또는 에러: error={}", provider, error);
                String loginUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/auth/" + provider + "/callback")
                    .queryParam("error", error != null ? error : "user_cancelled")
                    .build()
                    .encode()
                    .toUriString();
                response.sendRedirect(loginUrl);
                return ResponseEntity.ok().build();
            }
            
            LoginResponse loginResponse;
            String frontendCallbackUrl;
            
            switch (provider.toLowerCase()) {
                case "google":
                    loginResponse = googleOAuthService.handleCallback(code);
                    frontendCallbackUrl = "http://localhost:5173/auth/google/callback";
                    break;
                case "naver":
                    // TODO: NaverOAuthService 구현 시
                    throw new UnsupportedOperationException("Naver 로그인은 아직 지원되지 않습니다.");
                case "kakao":
                    // TODO: KakaoOAuthService 구현 시
                    throw new UnsupportedOperationException("Kakao 로그인은 아직 지원되지 않습니다.");
                default:
                    throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + provider);
            }
            
            // 프론트엔드로 리디렉션
            String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .queryParam("accessToken", loginResponse.getAccessToken())
                .queryParam("refreshToken", loginResponse.getRefreshToken())
                .queryParam("memberNo", loginResponse.getMemberNo())
                .queryParam("memberName", loginResponse.getMemberName())
                .queryParam("memberRole", loginResponse.getMemberRole())
                .queryParam("agencyNo", loginResponse.getAgencyNo() != null ? loginResponse.getAgencyNo() : "")
                .build()
                .encode()
                .toUriString();
            
            // Refresh Token을 쿠키에 저장 (일반 로그인과 동일하게)
            if (loginResponse.getRefreshToken() != null) {
                addRefreshTokenCookie(response, loginResponse.getRefreshToken());
                log.info("✅ [OAuth 콜백] Refresh Token 쿠키 저장 완료: provider={}", provider);
            }
            
            response.sendRedirect(redirectUrl);
            return ResponseEntity.ok().build();
            
        } catch (OAuthRegistrationRequiredException e) {
            // 신규 회원인 경우 회원가입 페이지로 리디렉션
            log.info("{} OAuth 신규 회원 감지: email={}, name={}", provider, e.getEmail(), e.getName());
            try {
                String signupUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/signup")
                    .queryParam("email", e.getEmail())
                    .queryParam("name", e.getName())
                    .queryParam("oauth", e.getProvider())
                    .build()
                    .encode()
                    .toUriString();
                response.sendRedirect(signupUrl);
            } catch (Exception ex) {
                log.error("회원가입 페이지 리디렉션 실패", ex);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("{} OAuth 콜백 처리 실패", provider, e);
            try {
                String errorUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/auth/" + provider + "/callback")
                    .queryParam("error", "oauth_login_failed")
                    .build()
                    .encode()
                    .toUriString();
                response.sendRedirect(errorUrl);
            } catch (Exception ex) {
                log.error("리디렉션 실패", ex);
            }
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Refresh Token을 쿠키에 저장
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        boolean isLocal = "local".equals(activeProfile);
        int maxAge = (int) (refreshExpiration / 1000);
        String secureFlag = isLocal ? "" : "Secure; ";
        
        String cookieValue = String.format(
            "refreshToken=%s; HttpOnly; Path=/; Max-Age=%d; %sSameSite=Strict",
            refreshToken,
            maxAge,
            secureFlag
        );
        response.addHeader("Set-Cookie", cookieValue);
    }
}
        