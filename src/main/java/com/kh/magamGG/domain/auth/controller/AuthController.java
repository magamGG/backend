package com.kh.magamGG.domain.auth.controller;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.request.RefreshTokenRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.dto.response.RefreshTokenResponse;
import com.kh.magamGG.domain.auth.service.AuthService;
import com.kh.magamGG.domain.auth.service.GoogleOAuthService;
import com.kh.magamGG.domain.auth.service.PasswordResetService;
import com.kh.magamGG.domain.auth.service.RefreshTokenService;
import com.kh.magamGG.global.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.kh.magamGG.global.exception.OAuthRegistrationRequiredException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * 
 * 주요 변경사항:
 * - /api/auth/reissue 엔드포인트 추가 (쿠키에서 Refresh Token 추출)
 * - Refresh Token Rotation 적용
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request);
        
        // Refresh Token을 쿠키에 저장 (reissue 시 쿠키에서 읽기 위해)
        // Valkey에는 이미 AuthService.login()에서 저장됨
        if (response.getRefreshToken() != null) {
            addRefreshTokenCookie(httpResponse, response.getRefreshToken());
            log.info("✅ [로그인] Refresh Token 쿠키 저장 완료");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 기존 /api/auth/refresh 엔드포인트 (하위 호환성 유지)
     * Request Body에서 Refresh Token을 받음
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 재발급 엔드포인트 (쿠키 기반)
     * 
     * 작동 원리:
     * 1. HttpOnly 쿠키에서 Refresh Token 추출
     * 2. Valkey에 저장된 토큰과 비교
     * 3. 일치하면 기존 토큰 삭제 후 새 토큰 발급 (Rotation)
     * 4. 새 Refresh Token을 쿠키에 저장 + Valkey에 저장
     * 5. 새 Access Token을 응답으로 반환
     * 
     * 보안 고려사항:
     * - 쿠키에서 토큰을 추출하므로 XSS 공격에 안전 (HttpOnly)
     * - 토큰 불일치 시 모든 세션 무효화
     */
    /**
     * 토큰 재발급 엔드포인트 (쿠키 기반)
     * 
     * 작동 원리:
     * 1. HttpOnly 쿠키에서 Refresh Token 추출
     * 2. Valkey에 저장된 토큰과 비교
     * 3. 일치하면 기존 토큰 삭제 후 새 토큰 발급 (Rotation)
     * 4. 새 Refresh Token을 쿠키에 저장 + Valkey에 저장
     * 5. 새 Access Token을 응답으로 반환
     * 
     * 보안 고려사항:
     * - 쿠키에서 토큰을 추출하므로 XSS 공격에 안전 (HttpOnly)
     * - 토큰 불일치 시 모든 세션 무효화
     * - 🔒 Strict 검증: Valkey에 키가 없으면 즉시 401 반환
     */
    @PostMapping("/reissue")
    public ResponseEntity<RefreshTokenResponse> reissueToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("🔄 [토큰 재발급] /api/auth/reissue 요청 시작");
        log.debug("🔄 [토큰 재발급] 요청 헤더 확인: Origin={}, Cookie={}", 
                request.getHeader("Origin"), 
                request.getHeader("Cookie") != null ? "있음" : "없음");

        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.error("❌ [토큰 재발급] 쿠키에서 Refresh Token을 찾을 수 없음");
            log.error("❌ [토큰 재발급] 쿠키 추출 실패 - 401 반환");
            return ResponseEntity.status(401)
                    .body(RefreshTokenResponse.builder()
                            .accessToken(null)
                            .refreshToken(null)
                            .build());
        }
        
        log.info("✅ [토큰 재발급] 쿠키에서 Refresh Token 추출 성공: 길이={}", refreshToken.length());

        // 2. Refresh Token 검증 (JWT 서명 및 만료 시간)
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("❌ [토큰 재발급] 유효하지 않은 Refresh Token (JWT 검증 실패)");
            return ResponseEntity.status(401)
                    .body(RefreshTokenResponse.builder()
                            .accessToken(null)
                            .refreshToken(null)
                            .build());
        }

        // 3. Refresh Token에서 회원번호 추출
        Long memberNo;
        try {
            memberNo = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        } catch (Exception e) {
            log.error("❌ [토큰 재발급] 토큰에서 회원번호 추출 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
        
        // 4. 회원 정보 조회 (이메일 가져오기)
        String email = authService.getMemberEmail(memberNo);
        if (email == null) {
            log.error("❌ [토큰 재발급] 회원을 찾을 수 없음: memberNo={}", memberNo);
            return ResponseEntity.status(401).build();
        }

        // 5. 🔒 Strict 검증: Valkey에 해당 키가 있는지 확인
        // 이미 로그아웃되어 삭제된 토큰인지 확인
        String key = "RT:" + email;
        log.debug("🔍 [토큰 재발급] Valkey 키 확인 시작: key={}", key);
        String storedTokenHash = refreshTokenService.getRefreshToken(email);
        if (storedTokenHash == null) {
            log.error("🔒 [보안 경고] Valkey에 Refresh Token이 없음 (이미 삭제됨): email={}, key={}", email, key);
            log.error("🔒 [보안 경고] 가능한 원인: 1) 로그아웃됨, 2) 토큰 만료, 3) Valkey 연결 문제");
            // 쿠키도 만료 처리
            expireRefreshTokenCookie(response);
            return ResponseEntity.status(401)
                    .body(RefreshTokenResponse.builder()
                            .accessToken(null)
                            .refreshToken(null)
                            .build());
        }
        log.info("✅ [토큰 재발급] Valkey에 Refresh Token 존재 확인: email={}, key={}, hashLength={}", 
                email, key, storedTokenHash != null ? storedTokenHash.length() : 0);

        // 6. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(memberNo, email);

        // 7. 새 Refresh Token 발급
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(memberNo);

        // 8. Refresh Token Rotation (Valkey 기반)
        // validateAndRotate: 기존 토큰 검증 → 삭제 → 새 토큰 저장 (원자적 연산)
        try {
            refreshTokenService.validateAndRotate(email, refreshToken, newRefreshToken);
        } catch (com.kh.magamGG.global.exception.InvalidTokenException e) {
            log.error("❌ [토큰 재발급] Rotation 실패: {}", e.getMessage());
            // 쿠키도 만료 처리
            expireRefreshTokenCookie(response);
            return ResponseEntity.status(401).build();
        }

        // 9. 새 Refresh Token을 쿠키에 저장
        addRefreshTokenCookie(response, newRefreshToken);

        log.info("✅ [토큰 재발급] 성공: email={}", email);

        // 10. 응답 반환
        return ResponseEntity.ok(RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(null)  // 쿠키에 저장되므로 응답에는 포함하지 않음
                .build());
    }

    /**
     * 쿠키에서 Refresh Token 추출
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.warn("❌ [쿠키 추출] 요청에 쿠키가 없음");
            return null;
        }

        log.debug("🍪 [쿠키 추출] 총 {}개의 쿠키 발견", cookies.length);
        for (Cookie cookie : cookies) {
            log.debug("🍪 [쿠키 추출] 쿠키 이름: {}, 값 길이: {}", 
                    cookie.getName(), cookie.getValue() != null ? cookie.getValue().length() : 0);
            if ("refreshToken".equals(cookie.getName())) {
                String tokenValue = cookie.getValue();
                log.info("✅ [쿠키 추출] Refresh Token 발견: 길이={}", 
                        tokenValue != null ? tokenValue.length() : 0);
                return tokenValue;
            }
        }

        log.warn("❌ [쿠키 추출] refreshToken 쿠키를 찾을 수 없음");
        return null;
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

    /**
     * 로그아웃 처리
     * 
     * 처리 과정:
     * 1. 쿠키에서 Refresh Token 추출
     * 2. Valkey에서 Refresh Token 즉시 삭제
     * 3. 쿠키 만료 처리 (Max-Age=0)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        log.info("🚪 [로그아웃] 요청 시작");
        
        // 쿠키에서 Refresh Token 추출 시도
        String refreshToken = extractRefreshTokenFromCookie(httpRequest);
        
        // Request Body에서도 시도 (하위 호환성)
        if (refreshToken == null && request != null) {
            refreshToken = request.getRefreshToken();
        }
        
        if (refreshToken != null) {
            // Valkey에서 Refresh Token 삭제
            authService.logout(refreshToken);
        }
        
        // 쿠키 만료 처리 (Max-Age=0으로 설정하여 즉시 삭제)
        expireRefreshTokenCookie(httpResponse);
        
        log.info("✅ [로그아웃] 완료");
        return ResponseEntity.ok().build();
    }

    /**
     * Refresh Token 쿠키 만료 처리
     * Max-Age=0으로 설정하여 브라우저에서 즉시 삭제
     */
    private void expireRefreshTokenCookie(HttpServletResponse response) {
        String cookieValue = "refreshToken=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict";
        response.addHeader("Set-Cookie", cookieValue);
        log.debug("🍪 Refresh Token 쿠키 만료 처리 완료");
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
        