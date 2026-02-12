package com.kh.magamGG.domain.inquiry.service;

import com.kh.magamGG.domain.inquiry.dto.request.InquiryRequest;
import com.kh.magamGG.domain.inquiry.entity.Inquiry;
import com.kh.magamGG.domain.inquiry.repository.InquiryRepository;
import com.kh.magamGG.global.service.EmailService;
import com.kh.magamGG.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    
    @Transactional
    public Inquiry createInquiry(Long memberNo, InquiryRequest request, List<MultipartFile> files) {
        // 1) 파일 저장 및 URL 수집
        StringBuilder attachmentUrls = new StringBuilder();
        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.saveFile(file);
                    if (i > 0) {
                        attachmentUrls.append(",");
                    }
                    attachmentUrls.append(fileName);
                }
            }
        }
        
        // 2) 문의 엔티티 생성
        Inquiry inquiry = Inquiry.builder()
            .memberNo(memberNo)
            .inquiryType(request.getInquiryType())
            .inquiryTitle(request.getTitle())
            .inquiryContent(request.getContent())
            .developerEmail(request.getDeveloperEmail() != null ? 
                request.getDeveloperEmail() : "magamgglocalservice@gmail.com")
            .attachmentUrls(attachmentUrls.length() > 0 ? attachmentUrls.toString() : null)
            .build();
        
        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        
        // 3) 이메일 전송 (MultipartFile 직접 사용)
        try {
            String emailContent = buildEmailContent(request, savedInquiry);
            emailService.sendEmail(
                savedInquiry.getDeveloperEmail(),
                "[문의] " + request.getTitle(),
                emailContent,
                files // MultipartFile 직접 전달
            );
            log.info("문의 이메일 전송 성공: inquiryNo={}, developerEmail={}", 
                     savedInquiry.getInquiryNo(), savedInquiry.getDeveloperEmail());
        } catch (Exception e) {
            log.error("문의 이메일 전송 실패: {}", e.getMessage(), e);
            // 이메일 전송 실패해도 문의 기록은 저장된 상태로 둔다
        }
        
        return savedInquiry;
    }
    
    private String buildEmailContent(InquiryRequest request, Inquiry inquiry) {
        return String.format(
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<h2 style='color: #3F4A5A;'>새로운 문의가 접수되었습니다</h2>" +
            "<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
            "<p><strong>문의 번호:</strong> %d</p>" +
            "<p><strong>문의 유형:</strong> %s</p>" +
            "<p><strong>제목:</strong> %s</p>" +
            "<p><strong>내용:</strong></p>" +
            "<div style='background-color: white; padding: 10px; border-radius: 3px; white-space: pre-wrap;'>%s</div>" +
            "<p><strong>접수 시간:</strong> %s</p>" +
            "</div>" +
            "<p style='color: #6E8FB3; font-size: 12px;'>이 메일은 magamGG 시스템에서 자동으로 전송되었습니다.</p>" +
            "</body></html>",
            inquiry.getInquiryNo(),
            getInquiryTypeLabel(request.getInquiryType()),
            request.getTitle(),
            request.getContent().replace("\n", "<br>"),
            inquiry.getInquiryCreatedAt()
        );
    }
    
    private String getInquiryTypeLabel(String type) {
        switch (type) {
            case "bug": return "버그 신고";
            case "feature": return "기능 요청";
            case "system": return "시스템 문의";
            case "other": return "기타";
            default: return type;
        }
    }
}

