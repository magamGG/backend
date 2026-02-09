package com.kh.magamGG.domain.health.controller;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.dto.response.HealthSurveyResponseStatusResponse;
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
     * HEALTH_SURVEY_QUESTION_TYPE으로 질문 목록 조회
     * 소속 상관없이 모든 문항 조회
     * 예: GET /api/health-surveys/type/월간 정신/questions
     */
    @GetMapping("/type/{healthSurveyType}/questions")
    public ResponseEntity<List<HealthSurveyQuestionResponse>> getQuestionsByType(
        @PathVariable String healthSurveyType
    ) {
        List<HealthSurveyQuestionResponse> questions =
            healthSurveyService.getQuestionsByType(healthSurveyType);
        return ResponseEntity.ok(questions);
    }

    /**
     * 설문 응답 제출
     * POST /api/health-surveys/responses
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
    @PostMapping("/responses")
    public ResponseEntity<HealthSurveySubmitResponse> submitSurveyResponse(
        @RequestBody HealthSurveySubmitRequest request
    ) {
        HealthSurveySubmitResponse response =
            healthSurveyService.submitSurveyResponse(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원의 특정 HEALTH_SURVEY_QUESTION_TYPE에 대한 응답 상태 조회
     * GET /api/health-surveys/member/{memberNo}/responses?type={healthSurveyQuestionType}
     * 
     * 예: GET /api/health-surveys/member/123/responses?type=월간 정신
     */
    @GetMapping("/member/{memberNo}/responses")
    public ResponseEntity<HealthSurveyResponseStatusResponse> getSurveyResponseStatus(
        @PathVariable Long memberNo,
        @RequestParam String type
    ) {
        HealthSurveyResponseStatusResponse response =
            healthSurveyService.getSurveyResponseStatus(memberNo, type);
        return ResponseEntity.ok(response);
    }
}


