package com.kh.magamGG.domain.portfolio.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 포트폴리오 엔티티 (PORTFOLIO 테이블)
 * - 내부 아티스트: MEMBER_NO로 연결
 * - 외부 포트폴리오: MEMBER_NO NULL, 작성자 정보 직접 저장
 */
@Entity
@Table(name = "PORTFOLIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PORTFOLIO_NO")
    private Long portfolioNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = true)
    private Member member;

    @Column(name = "PORTFOLIO_USER_NAME", length = 50)
    private String portfolioUserName;

    @Column(name = "PORTFOLIO_USER_PHONE", length = 20)
    private String portfolioUserPhone;

    @Column(name = "PORTFOLIO_USER_EMAIL", length = 100)
    private String portfolioUserEmail;

    @Column(name = "PORTFOLIO_USER_CAREER", length = 2000)
    private String portfolioUserCareer;

    @Column(name = "PORTFOLIO_USER_PROJECT", length = 2000)
    private String portfolioUserProject;

    @Column(name = "PORTFOLIO_USER_SKILL", length = 2000)
    private String portfolioUserSkill;

    @Column(name = "PORTFOLIO_STATUS", nullable = false, length = 2)
    @Builder.Default
    private String portfolioStatus = "Y";

    @Column(name = "PORTFOLIO_CREATED_AT", nullable = false)
    private LocalDateTime portfolioCreatedAt;

    @Column(name = "PORTFOLIO_UPDATED_AT")
    private LocalDateTime portfolioUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (portfolioCreatedAt == null) {
            portfolioCreatedAt = LocalDateTime.now();
        }
        if (portfolioStatus == null) {
            portfolioStatus = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        portfolioUpdatedAt = LocalDateTime.now();
    }
}
