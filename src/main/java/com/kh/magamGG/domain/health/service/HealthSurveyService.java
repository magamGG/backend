package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.HealthSurveyQuestionResponseDto;

import java.util.List;

public interface HealthSurveyService {

    /**
     * 설문 번호로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyNo(Long healthSurveyNo);

    /**
     * HEALTH_SURVEY_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyType(String healthSurveyType);

    /**
     * 설문 이름(예: PHQ-9, GAD, QuickDASH)으로 질문 목록 조회
     */
    List<HealthSurveyQuestionResponseDto> getQuestionsBySurveyName(String healthSurveyName);
}


