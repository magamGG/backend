package com.kh.magamGG.domain.health.controller;

import com.kh.magamGG.domain.health.dto.HealthSurveyQuestionResponseDto;
import com.kh.magamGG.domain.health.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health-surveys")
@RequiredArgsConstructor
public class HealthSurveyController {

    private final HealthSurveyService healthSurveyService;

    /**
     * 설문 번호로 질문 목록 조회
     * 예: GET /api/health-surveys/1/questions
     */
    @GetMapping("/{healthSurveyNo}/questions")
    public ResponseEntity<List<HealthSurveyQuestionResponseDto>> getQuestionsBySurveyNo(
        @PathVariable Long healthSurveyNo
    ) {
        List<HealthSurveyQuestionResponseDto> questions =
            healthSurveyService.getQuestionsBySurveyNo(healthSurveyNo);
        return ResponseEntity.ok(questions);
    }

    /**
     * HEALTH_SURVEY_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 질문 목록 조회
     * 예: GET /api/health-surveys/type/월간 정신/questions
     */
    @GetMapping("/type/{healthSurveyType}/questions")
    public ResponseEntity<List<HealthSurveyQuestionResponseDto>> getQuestionsBySurveyType(
        @PathVariable String healthSurveyType
    ) {
        List<HealthSurveyQuestionResponseDto> questions =
            healthSurveyService.getQuestionsBySurveyType(healthSurveyType);
        return ResponseEntity.ok(questions);
    }

    /**
     * 설문 이름(PHQ-9, GAD, QuickDASH 등)으로 질문 목록 조회
     * 예: GET /api/health-surveys/name/PHQ-9/questions
     */
    @GetMapping("/name/{healthSurveyName}/questions")
    public ResponseEntity<List<HealthSurveyQuestionResponseDto>> getQuestionsBySurveyName(
        @PathVariable String healthSurveyName
    ) {
        List<HealthSurveyQuestionResponseDto> questions =
            healthSurveyService.getQuestionsBySurveyName(healthSurveyName);
        return ResponseEntity.ok(questions);
    }
}


