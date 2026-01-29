package com.kh.magamGG.domain.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveyQuestionResponseDto {

    private Long healthSurveyQuestionNo;
    private Long healthSurveyNo;
    private Integer healthSurveyOrder;
    private String healthSurveyQuestionContent;
    private Integer healthSurveyQuestionMinScore;
    private Integer healthSurveyQuestionMaxScore;
}


