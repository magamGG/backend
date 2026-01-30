package com.kh.magamGG.domain.health.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveyAnswerRequest {
    private Long questionId;   // HEALTH_SURVEY_QUESTION_NO
    private Integer score;     // 사용자가 선택한 점수 (0~3, 0~4, 1~5, 0~10 등)
}

