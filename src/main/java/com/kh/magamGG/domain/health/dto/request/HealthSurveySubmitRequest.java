package com.kh.magamGG.domain.health.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthSurveySubmitRequest {
    private Long memberNo;                 // 응답자 회원 번호
    private List<HealthSurveyAnswerRequest> answers;
}

