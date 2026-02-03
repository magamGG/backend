package com.kh.magamGG.domain.health.controller;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<HealthSurveyQuestionResponse>> getQuestionsBySurveyNo(
        @PathVariable Long healthSurveyNo
    ) {
        List<HealthSurveyQuestionResponse> questions =
            healthSurveyService.getQuestionsBySurveyNo(healthSurveyNo);
        return ResponseEntity.ok(questions);
    }

    /**
     * AgencyNo와 HEALTH_SURVEY_QUESTION_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 질문 목록 조회
     * 해당 에이전시의 설문만 조회
     * 예: GET /api/health-surveys/type/월간 정신/questions?agencyNo=1
     */
    @GetMapping("/type/{healthSurveyType}/questions")
    public ResponseEntity<List<HealthSurveyQuestionResponse>> getQuestionsBySurveyType(
        @PathVariable String healthSurveyType,
        @RequestParam Long agencyNo
    ) {
        List<HealthSurveyQuestionResponse> questions =
            healthSurveyService.getQuestionsBySurveyType(agencyNo, healthSurveyType);
        return ResponseEntity.ok(questions);
    }

    /**
     * 설문 응답 제출
     * POST /api/health-surveys/{healthSurveyNo}/responses
     * 
     * 요청 예시:
     * {
     *   "memberNo": 123,
     *   "answers": [
     *     { "questionId": 1, "score": 2 },
     *     { "questionId": 2, "score": 3 }
     *   ]
     * }
     */
    @PostMapping("/{healthSurveyNo}/responses")
    public ResponseEntity<HealthSurveySubmitResponse> submitSurveyResponse(
        @PathVariable Long healthSurveyNo,
        @RequestBody HealthSurveySubmitRequest request
    ) {
        HealthSurveySubmitResponse response =
            healthSurveyService.submitSurveyResponse(healthSurveyNo, request);
        return ResponseEntity.ok(response);
    }
}


