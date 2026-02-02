package com.kh.magamGG.domain.health.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 건강 설문 응답 제출 요청 DTO
 * - 프론트엔드에서 각 문항별 점수의 총합을 계산하여 전송
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveySubmitRequest {
    private Long memberNo;                 // 응답자 회원 번호
    private Integer totalScore;             // 총점 (프론트엔드에서 각 문항별 점수의 총합으로 계산된 값)
}

