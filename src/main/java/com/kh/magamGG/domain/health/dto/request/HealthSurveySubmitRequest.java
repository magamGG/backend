package com.kh.magamGG.domain.health.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveySubmitRequest {
    private Long memberNo;                 // 응답자 회원 번호
    private Integer totalScore;             // 총점 (프론트엔드에서 계산된 값)
}

