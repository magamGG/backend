package com.kh.magamGG.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveySubmitResponse {
    // healthSurveyNo 제거 (더 이상 필요 없음)
    private Long memberNo;
    private int totalScore;
    private String riskLevel;  // "정상", "주의", "경고", "위험"
}

