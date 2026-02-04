package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.dto.response.HealthSurveyResponseStatusResponse;

import java.util.List;

public interface HealthSurveyService {

    /**
     * HEALTH_SURVEY_QUESTION_TYPE으로 질문 목록 조회
     * 소속 상관없이 모든 문항 조회
     */
    List<HealthSurveyQuestionResponse> getQuestionsByType(String healthSurveyType);

    /**
     * 설문 응답 제출 (문항별 점수 → 총점 계산 + 등급 산정 + DB 저장)
     */
    HealthSurveySubmitResponse submitSurveyResponse(HealthSurveySubmitRequest request);

    /**
     * 설문 타입과 총점을 받아 위험도 등급 계산
     */
    String evaluateRiskLevel(String healthSurveyType, int totalScore);

    /**
     * 회원의 특정 설문 타입에 대한 응답 상태 조회
     * @param memberNo 회원 번호
     * @param healthSurveyQuestionType HEALTH_SURVEY_QUESTION_TYPE ("월간 정신", "월간 신체" 등)
     * @return 설문 완료 여부, 마지막 검사 일자, 총점, 위험도 등급
     */
    HealthSurveyResponseStatusResponse getSurveyResponseStatus(Long memberNo, String healthSurveyQuestionType);
}


