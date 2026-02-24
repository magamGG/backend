package com.kh.magamGG.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioUpdateRequest {

    private String portfolioUserName;
    private String portfolioUserPhone;
    private String portfolioUserEmail;
    private String portfolioUserCareer;
    private String portfolioUserProject;
    private String portfolioUserSkill;
}
