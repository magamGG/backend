package com.kh.magamGG.domain.inquiry.dto.response;

import com.kh.magamGG.domain.inquiry.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryResponse {
    private Long inquiryNo;
    private Long memberNo;
    private String inquiryType;
    private String inquiryTitle;
    private String inquiryContent;
    private String inquiryStatus;
    private String developerEmail;
    private String attachmentUrls;
    private LocalDateTime inquiryCreatedAt;
    private LocalDateTime inquiryUpdatedAt;
    
    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .inquiryNo(inquiry.getInquiryNo())
                .memberNo(inquiry.getMemberNo())
                .inquiryType(inquiry.getInquiryType())
                .inquiryTitle(inquiry.getInquiryTitle())
                .inquiryContent(inquiry.getInquiryContent())
                .inquiryStatus(inquiry.getInquiryStatus())
                .developerEmail(inquiry.getDeveloperEmail())
                .attachmentUrls(inquiry.getAttachmentUrls())
                .inquiryCreatedAt(inquiry.getInquiryCreatedAt())
                .inquiryUpdatedAt(inquiry.getInquiryUpdatedAt())
                .build();
    }
}

