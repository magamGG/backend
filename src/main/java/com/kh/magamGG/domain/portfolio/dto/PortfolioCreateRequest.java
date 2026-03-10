package com.kh.magamGG.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포트폴리오 생성 요청 (직접 작성 또는 추출 결과 저장)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioCreateRequest {

    private String portfolioUserName;
    private String portfolioUserPhone;
    private String portfolioUserEmail;
    private String portfolioUserCareer;
    private String portfolioUserProject;
    private String portfolioUserSkill;
}
