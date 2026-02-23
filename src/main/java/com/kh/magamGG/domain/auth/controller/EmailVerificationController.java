package com.kh.magamGG.domain.auth.controller;

import com.kh.magamGG.domain.auth.service.EmailVerificationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {
    
    private final EmailVerificationService emailVerificationService;
    
    /**
     * 인증 코드 전송
     */
    @PostMapping("/send-code")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody EmailRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }
    
    /**
     * 인증 코드 검증
     */
    @PostMapping("/verify-code")
    public ResponseEntity<VerifyResponse> verifyCode(@RequestBody VerifyRequest request) {
        boolean isValid = emailVerificationService.verifyCode(
            request.getEmail(), 
            request.getCode()
        );
        
        VerifyResponse response = new VerifyResponse();
        response.setVerified(isValid);
        response.setMessage(isValid ? "인증이 완료되었습니다." : "인증 코드가 올바르지 않거나 만료되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    @Getter
    @Setter
    public static class EmailRequest {
        private String email;
    }
    
    @Getter
    @Setter
    public static class VerifyRequest {
        private String email;
        private String code;
    }
    
    @Getter
    @Setter
    public static class VerifyResponse {
        private boolean verified;
        private String message;
    }
}

