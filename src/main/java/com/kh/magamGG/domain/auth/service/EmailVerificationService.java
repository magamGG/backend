package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.global.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    
    private final EmailService emailService;
    
    // 인증 코드 저장 (이메일: {코드, 만료시간})
    private final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();
    
    // 인증 완료된 이메일 저장
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();
    
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 3;
    
    /**
     * 인증 코드 생성 및 전송
     */
    public void sendVerificationCode(String email) {
        // 6자리 숫자 코드 생성
        String code = generateCode();
        
        // 만료 시간 설정 (3분)
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
        
        // 저장
        verificationCodes.put(email, new VerificationCode(code, expiryTime));
        
        // 이메일 전송
        String emailContent = buildVerificationEmail(code);
        emailService.sendEmail(email, "[마감지기] 이메일 인증 코드", emailContent);
        
        log.info("인증 코드 전송: email={}, code={}", email, code);
    }
    
    /**
     * 인증 코드 검증
     */
    public boolean verifyCode(String email, String code) {
        VerificationCode stored = verificationCodes.get(email);
        
        if (stored == null) {
            log.warn("인증 코드 없음: email={}", email);
            return false;
        }
        
        // 만료 확인
        if (LocalDateTime.now().isAfter(stored.expiryTime)) {
            verificationCodes.remove(email);
            log.warn("인증 코드 만료: email={}", email);
            return false;
        }
        
        // 코드 일치 확인
        if (!stored.code.equals(code)) {
            log.warn("인증 코드 불일치: email={}", email);
            return false;
        }
        
        // 인증 성공
        verificationCodes.remove(email);
        verifiedEmails.put(email, true);
        log.info("인증 코드 검증 성공: email={}", email);
        return true;
    }
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }
    
    /**
     * 인증 완료 상태 제거 (회원가입 완료 후)
     */
    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }
    
    /**
     * 6자리 숫자 코드 생성
     */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 인증 이메일 내용 생성
     */
    private String buildVerificationEmail(String code) {
        return String.format(
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<h2 style='color: #3F4A5A;'>이메일 인증 코드</h2>" +
            "<div style='background-color: #f5f5f5; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
            "<p style='font-size: 18px; color: #1F2328;'>인증 코드: <strong style='color: #3F4A5A; font-size: 24px; letter-spacing: 3px;'>%s</strong></p>" +
            "<p style='color: #6E8FB3; font-size: 14px;'>이 코드는 3분간 유효합니다.</p>" +
            "</div>" +
            "<p style='color: #6E8FB3; font-size: 12px;'>이 메일은 마감지기 회원가입 인증을 위해 전송되었습니다.</p>" +
            "</body></html>",
            code
        );
    }
    
    /**
     * 인증 코드 정보 클래스
     */
    private static class VerificationCode {
        String code;
        LocalDateTime expiryTime;
        
        VerificationCode(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}

