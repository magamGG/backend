package com.kh.magamGG.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveyResponseStatusResponse {
    private boolean isCompleted;  // 설문 완료 여부
    private LocalDateTime lastCheckDate;  // 마지막 검사 일자
    private Integer totalScore;  // 총점
    private String riskLevel;  // 위험도 등급 ("정상", "주의", "경고", "위험")
    private Integer healthSurveyPeriod;  // 검사 기간 (일)
    private Integer healthSurveyCycle;  // 검사 주기 (일)
    private LocalDateTime nextCheckupDate;  // 다음 검진일
    private Integer daysRemaining;  // 남은 일수
    private LocalDateTime deadlineDate;  // 마감일 (미검진 상태일 때)
    // healthSurveyType 필드 제거 (프론트엔드에서 사용하지 않음, HEALTH_SURVEY_QUESTION_TYPE과 혼동 방지)
}

