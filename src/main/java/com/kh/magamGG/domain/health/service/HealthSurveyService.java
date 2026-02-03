package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;

import java.util.List;

public interface HealthSurveyService {

    /**
     * 설문 번호로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponse> getQuestionsBySurveyNo(Long healthSurveyNo);

    /**
     * HEALTH_SURVEY_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponse> getQuestionsByAgencyNo(Long agencyNo);

    /**
     * 설문 응답 제출 (문항별 점수 → 총점 계산 + 등급 산정 + DB 저장)
     */
    HealthSurveySubmitResponse submitSurveyResponse(Long healthSurveyNo, HealthSurveySubmitRequest request);

    /**
     * 설문 타입과 총점을 받아 위험도 등급 계산
     */
    String evaluateRiskLevel(String healthSurveyType, int totalScore);
}


