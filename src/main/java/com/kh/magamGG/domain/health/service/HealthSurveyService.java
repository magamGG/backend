package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.dto.response.HealthSurveyResponseStatusResponse;

import java.util.List;

public interface HealthSurveyService {

    /**
     * 설문 번호로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponse> getQuestionsBySurveyNo(Long healthSurveyNo);

    /**
     * AgencyNo와 HEALTH_SURVEY_QUESTION_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 질문 목록 조회
     * 해당 에이전시의 설문만 조회
     */
    List<HealthSurveyQuestionResponse> getQuestionsBySurveyType(Long agencyNo, String healthSurveyType);

    /**
     * 설문 응답 제출 (문항별 점수 → 총점 계산 + 등급 산정 + DB 저장)
     */
    HealthSurveySubmitResponse submitSurveyResponse(Long healthSurveyNo, HealthSurveySubmitRequest request);

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


