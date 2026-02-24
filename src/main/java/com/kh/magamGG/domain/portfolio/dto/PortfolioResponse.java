package com.kh.magamGG.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포트폴리오 조회 응답 (규격)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {

    private Long portfolioNo;
    private Long memberNo;
    private String portfolioUserName;
    private String portfolioUserPhone;
    private String portfolioUserEmail;
    private String portfolioUserCareer;
    private String portfolioUserProject;
    private String portfolioUserSkill;
    private String portfolioStatus;
    private LocalDateTime portfolioCreatedAt;
    private LocalDateTime portfolioUpdatedAt;
}
