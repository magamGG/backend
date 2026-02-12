package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import com.kh.magamGG.global.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final EmailService emailService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 비밀번호 재설정용 인증 코드 저장 (이메일: {코드, 만료시간})
    private final Map<String, VerificationCode> resetCodes = new ConcurrentHashMap<>();
    
    // 인증 완료된 이메일 저장
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();
    
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 3;
    
    /**
     * 비밀번호 재설정 요청 (인증 코드 전송)
     * 보안: 회원 존재 여부와 관계없이 성공 메시지 반환
     */
    public void requestPasswordReset(String email) {
        // 회원 존재 여부 확인 (보안을 위해 존재하지 않아도 성공 메시지 반환)
        boolean memberExists = memberRepository.existsByMemberEmail(email);
        
        if (!memberExists) {
            log.warn("비밀번호 재설정 요청: 존재하지 않는 이메일 - {}", email);
            // 보안상 존재하지 않는 이메일이어도 성공 메시지 반환
            return;
        }
        
        // 6자리 숫자 코드 생성
        String code = generateCode();
        
        // 만료 시간 설정 (3분)
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
        
        // 저장
        resetCodes.put(email, new VerificationCode(code, expiryTime));
        
        // 이메일 전송
        String emailContent = buildPasswordResetEmail(code);
        emailService.sendEmail(email, "[마감지기] 비밀번호 재설정 인증 코드", emailContent);
        
        log.info("비밀번호 재설정 인증 코드 전송: email={}, code={}", email, code);
    }
    
    /**
     * 비밀번호 재설정용 인증 코드 검증
     */
    public boolean verifyResetCode(String email, String code) {
        VerificationCode stored = resetCodes.get(email);
        
        if (stored == null) {
            log.warn("인증 코드 없음: email={}", email);
            return false;
        }
        
        // 만료 확인
        if (LocalDateTime.now().isAfter(stored.expiryTime)) {
            resetCodes.remove(email);
            log.warn("인증 코드 만료: email={}", email);
            return false;
        }
        
        // 코드 일치 확인
        if (!stored.code.equals(code)) {
            log.warn("인증 코드 불일치: email={}", email);
            return false;
        }
        
        // 인증 성공
        resetCodes.remove(email);
        verifiedEmails.put(email, true);
        log.info("비밀번호 재설정 인증 코드 검증 성공: email={}", email);
        return true;
    }
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }
    
    /**
     * 인증 완료 상태 제거 (비밀번호 재설정 완료 후)
     */
    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }
    
    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // 1. 인증 완료 여부 확인
        if (!isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }
        
        // 2. 회원 조회
        Member member = memberRepository.findByMemberEmail(email)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 이메일입니다."));
        
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // 4. 비밀번호 업데이트
        member.updatePassword(encodedPassword);
        memberRepository.save(member);
        
        // 5. 인증 완료 상태 제거
        removeVerifiedEmail(email);
        
        log.info("비밀번호 재설정 완료: email={}", email);
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
     * 비밀번호 재설정 이메일 내용 생성
     */
    private String buildPasswordResetEmail(String code) {
        return String.format(
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<h2 style='color: #3F4A5A;'>비밀번호 재설정 인증 코드</h2>" +
            "<div style='background-color: #f5f5f5; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
            "<p style='font-size: 18px; color: #1F2328;'>인증 코드: <strong style='color: #3F4A5A; font-size: 24px; letter-spacing: 3px;'>%s</strong></p>" +
            "<p style='color: #6E8FB3; font-size: 14px;'>이 코드는 3분간 유효합니다.</p>" +
            "</div>" +
            "<p style='color: #6E8FB3; font-size: 12px;'>이 메일은 마감지기 비밀번호 재설정을 위해 전송되었습니다.</p>" +
            "<p style='color: #d32f2f; font-size: 12px;'>본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>" +
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

