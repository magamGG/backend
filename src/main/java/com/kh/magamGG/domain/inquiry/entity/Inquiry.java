package com.kh.magamGG.domain.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INQUIRY_NO")
    private Long inquiryNo;
    
    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;
    
    @Column(name = "INQUIRY_TYPE", nullable = false, length = 20)
    private String inquiryType; // bug, feature, system, other
    
    @Column(name = "INQUIRY_TITLE", nullable = false, length = 200)
    private String inquiryTitle;
    
    @Column(name = "INQUIRY_CONTENT", nullable = false, length = 500)
    private String inquiryContent;
    
    @Column(name = "INQUIRY_STATUS", nullable = false, length = 20)
    private String inquiryStatus; // PENDING, COMPLETED
    
    @Column(name = "DEVELOPER_EMAIL", nullable = false, length = 100)
    private String developerEmail;
    
    // 첨부파일 URL (여러 개일 경우 콤마로 구분)
    @Column(name = "ATTACHMENT_URLS", length = 2000)
    private String attachmentUrls; // 예: "file1.jpg,file2.pdf"
    
    @Column(name = "INQUIRY_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime inquiryCreatedAt;
    
    @Column(name = "INQUIRY_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime inquiryUpdatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (inquiryCreatedAt == null) {
            inquiryCreatedAt = LocalDateTime.now();
        }
        if (inquiryStatus == null) {
            inquiryStatus = "PENDING";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        inquiryUpdatedAt = LocalDateTime.now();
    }
}

