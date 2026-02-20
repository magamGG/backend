package com.kh.magamGG.domain.auth.controller;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.request.RefreshTokenRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.dto.response.RefreshTokenResponse;
import com.kh.magamGG.domain.auth.service.AuthService;
import com.kh.magamGG.domain.auth.service.GoogleOAuthService;
import com.kh.magamGG.domain.auth.service.PasswordResetService;
import com.kh.magamGG.global.exception.OAuthRegistrationRequiredException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 찾기 요청 (인증 코드 전송)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 재설정용 인증 코드 검증
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<VerifyResponse> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        boolean isValid = passwordResetService.verifyResetCode(
            request.getEmail(), 
            request.getCode()
        );
        
        VerifyResponse response = new VerifyResponse();
        response.setVerified(isValid);
        response.setMessage(isValid ? "인증이 완료되었습니다." : "인증 코드가 올바르지 않거나 만료되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
            request.getEmail(), 
            request.getCode(), 
            request.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    @Getter
    @Setter
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Getter
    @Setter
    public static class VerifyResetCodeRequest {
        private String email;
        private String code;
    }

    @Getter
    @Setter
    public static class VerifyResponse {
        private boolean verified;
        private String message;
    }

    @Getter
    @Setter
    public static class ResetPasswordRequest {
        private String email;
        private String code;
        private String newPassword;
    }

    /**
     * OAuth 인증 URL 조회 (범용)
     * 지원: google, naver, kakao
     */
    @GetMapping("/{provider}/authorization-url")
    public ResponseEntity<Map<String, String>> getOAuthAuthorizationUrl(
            @PathVariable String provider) {
        String url;
        switch (provider.toLowerCase()) {
            case "google":
                url = googleOAuthService.getAuthorizationUrl();
                break;
            case "naver":
                // TODO: NaverOAuthService 구현 시
                // url = naverOAuthService.getAuthorizationUrl();
                throw new UnsupportedOperationException("Naver 로그인은 아직 지원되지 않습니다.");
            case "kakao":
                // TODO: KakaoOAuthService 구현 시
                // url = kakaoOAuthService.getAuthorizationUrl();
                throw new UnsupportedOperationException("Kakao 로그인은 아직 지원되지 않습니다.");
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + provider);
        }
        return ResponseEntity.ok(Map.of("authorizationUrl", url));
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
                    // loginResponse = naverOAuthService.handleCallback(code);
                    // frontendCallbackUrl = "http://localhost:5173/auth/naver/callback";
                    throw new UnsupportedOperationException("Naver 로그인은 아직 지원되지 않습니다.");
                case "kakao":
                    // TODO: KakaoOAuthService 구현 시
                    // loginResponse = kakaoOAuthService.handleCallback(code);
                    // frontendCallbackUrl = "http://localhost:5173/auth/kakao/callback";
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
                .build()  // false (기본값)로 빌드
                .encode()  // 빌드 후 인코딩 적용 (한글 처리)
                .toUriString();
            
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
                    .build()  // false (기본값)로 빌드
                    .encode()  // 빌드 후 인코딩 적용
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
                    .build()  // false (기본값)로 빌드
                    .encode()  // 빌드 후 인코딩 적용
                    .toUriString();
                response.sendRedirect(errorUrl);
            } catch (Exception ex) {
                log.error("리디렉션 실패", ex);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
        