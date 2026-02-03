package com.kh.magamGG.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveyQuestionResponse {

    private Long healthSurveyQuestionNo;
    // healthSurveyNo 제거 (소속 상관없이 모든 문항 공통 사용)
    private Integer healthSurveyOrder;
    private String healthSurveyQuestionContent;
    private String healthSurveyQuestionType;
    private Integer healthSurveyQuestionMinScore;
    private Integer healthSurveyQuestionMaxScore;
}


