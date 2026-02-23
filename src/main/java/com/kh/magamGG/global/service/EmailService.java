package com.kh.magamGG.global.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    
    /**
     * 이메일 전송 (첨부파일 포함 가능)
     */
    public void sendEmail(String to, String subject, String content, List<MultipartFile> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("magamgglocalservice@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // HTML 형식 지원
            
            // 첨부파일 추가
            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        helper.addAttachment(file.getOriginalFilename(), file);
                    }
                }
            }
            
            mailSender.send(message);
            log.info("이메일 전송 성공: {}", to);
        } catch (MessagingException e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
    
    /**
     * 이메일 전송 (첨부파일 없음)
     */
    public void sendEmail(String to, String subject, String content) {
        sendEmail(to, subject, content, null);
    }
}

