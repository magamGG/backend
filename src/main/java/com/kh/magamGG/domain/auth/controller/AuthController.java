package com.kh.magamGG.domain.auth.controller;

import com.kh.magamGG.domain.auth.dto.request.LoginRequest;
import com.kh.magamGG.domain.auth.dto.request.RefreshTokenRequest;
import com.kh.magamGG.domain.auth.dto.response.LoginResponse;
import com.kh.magamGG.domain.auth.dto.response.RefreshTokenResponse;
import com.kh.magamGG.domain.auth.service.AuthService;
import com.kh.magamGG.domain.auth.service.PasswordResetService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

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
}
        