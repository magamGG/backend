package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.HealthSurveyRiskLevelDto;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.entity.HealthSurvey;
import com.kh.magamGG.domain.health.entity.HealthSurveyQuestion;
import com.kh.magamGG.domain.health.entity.HealthSurveyResponseItem;
import com.kh.magamGG.domain.health.repository.HealthSurveyQuestionRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyResponseItemRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthSurveyServiceImpl implements HealthSurveyService {

    private final HealthSurveyQuestionRepository healthSurveyQuestionRepository;
    private final HealthSurveyRepository healthSurveyRepository;
    private final HealthSurveyResponseItemRepository healthSurveyResponseItemRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<HealthSurveyQuestionResponse> getQuestionsBySurveyNo(Long healthSurveyNo) {
        List<HealthSurveyQuestion> questions =
            healthSurveyQuestionRepository.findByHealthSurvey_HealthSurveyNoOrderByHealthSurveyOrderAsc(healthSurveyNo);

        return questions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<HealthSurveyQuestionResponse> getQuestionsBySurveyType(String healthSurveyType) {
        List<HealthSurveyQuestion> questions =
            healthSurveyQuestionRepository.findByHealthSurvey_HealthSurveyTypeOrderByHealthSurveyOrderAsc(healthSurveyType);

        return questions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<HealthSurveyQuestionResponse> getQuestionsBySurveyName(String healthSurveyName) {
        HealthSurvey survey = healthSurveyRepository.findByHealthSurveyName(healthSurveyName)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다: " + healthSurveyName));

        return getQuestionsBySurveyNo(survey.getHealthSurveyNo());
    }

    /**
     * 건강 설문 응답 제출
     * - 프론트엔드에서 각 문항별 점수의 총합을 계산하여 전송
     * - 백엔드에서는 총점만 받아서 HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE에 저장
     * - HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT의 유무로 검진 완료 여부 판단
     */
    @Override
    @Transactional
    public HealthSurveySubmitResponse submitSurveyResponse(Long healthSurveyNo, HealthSurveySubmitRequest request) {

        // 1. 설문, 회원 조회
        HealthSurvey survey = healthSurveyRepository.findById(healthSurveyNo)
            .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다: " + healthSurveyNo));

        Member member = memberRepository.findById(request.getMemberNo())
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + request.getMemberNo()));

        // 2. 총점 검증 (프론트엔드에서 계산된 각 문항별 점수의 총합)
        Integer totalScore = request.getTotalScore();
        if (totalScore == null || totalScore < 0) {
            throw new IllegalArgumentException("총점이 유효하지 않습니다: " + totalScore);
        }

        // 3. 설문의 첫 번째 문항 조회 (HEALTH_SURVEY_QUESTION_NO FK 제약조건 만족용)
        List<HealthSurveyQuestion> questions = 
            healthSurveyQuestionRepository.findByHealthSurvey_HealthSurveyNoOrderByHealthSurveyOrderAsc(healthSurveyNo);
        
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("설문에 문항이 없습니다: " + healthSurveyNo);
        }
        
        HealthSurveyQuestion firstQuestion = questions.get(0);

        // 4. 건강 설문 응답 저장
        // - HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE: 각 문항별 점수의 총합 저장
        // - HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT: 생성일 유무로 검진 완료 여부 판단
        LocalDateTime responseCreatedAt = LocalDateTime.now();
        
        HealthSurveyResponseItem item = new HealthSurveyResponseItem();
        item.setMember(member);
        item.setHealthSurveyQuestion(firstQuestion);  // FK 제약조건 만족 (어떤 문항이든 참조 가능)
        item.setHealthSurveyQuestionItemAnswerScore(totalScore);  // 각 문항별 점수의 총합 저장
        item.setHealthSurveyQuestionItemCreatedAt(responseCreatedAt);  // 검진 완료 여부 판단용

        healthSurveyResponseItemRepository.save(item);

        // 5. 총점/위험도 등급 계산
        String surveyType = survey.getHealthSurveyType(); // "데일리 정신" / "데일리 신체" / "월간 정신" / "월간 신체"
        String riskLevel = evaluateRiskLevel(surveyType, totalScore);

        // 6. 클라이언트로 반환
        return HealthSurveySubmitResponse.builder()
            .healthSurveyNo(healthSurveyNo)
            .memberNo(member.getMemberNo())
            .totalScore(totalScore)
            .riskLevel(riskLevel)
            .build();
    }

    @Override
    public String evaluateRiskLevel(String healthSurveyType, int totalScore) {
        if (healthSurveyType == null) {
            throw new IllegalArgumentException("HEALTH_SURVEY_TYPE 이 null 입니다.");
        }

        switch (healthSurveyType) {
            case "월간 정신":
                return evaluateMonthlyMental(totalScore);
            case "월간 신체":
                return evaluateMonthlyPhysicalQuickDash(totalScore);
            case "데일리 정신":
                return evaluateDailyMental(totalScore);
            case "데일리 신체":
                return evaluateDailyPhysical(totalScore);
            default:
                throw new IllegalArgumentException("지원하지 않는 HEALTH_SURVEY_TYPE 입니다: " + healthSurveyType);
        }
    }

    // ===== 점수 구간별 등급 로직 =====

    /**
     * 월간 정신 (PHQ-9 + 불안, 총점 0~67 가정)
     * 0~14: 정상, 15~29: 주의, 30~44: 경고, 45+: 위험
     */
    private String evaluateMonthlyMental(int totalScore) {
        if (totalScore <= 14) {
            return HealthSurveyRiskLevelDto.NORMAL;
        } else if (totalScore <= 29) {
            return HealthSurveyRiskLevelDto.CAUTION;
        } else if (totalScore <= 44) {
            return HealthSurveyRiskLevelDto.WARNING;
        } else {
            return HealthSurveyRiskLevelDto.DANGER;
        }
    }

    /**
     * 월간 신체 (QuickDASH, 총점 11~55 가정)
     * 11~20: 정상, 21~30: 주의, 31~40: 경고, 41+: 위험
     */
    private String evaluateMonthlyPhysicalQuickDash(int totalScore) {
        if (totalScore <= 20) {
            return HealthSurveyRiskLevelDto.NORMAL;
        } else if (totalScore <= 30) {
            return HealthSurveyRiskLevelDto.CAUTION;
        } else if (totalScore <= 40) {
            return HealthSurveyRiskLevelDto.WARNING;
        } else {
            return HealthSurveyRiskLevelDto.DANGER;
        }
    }

    /**
     * 데일리 정신 (0~40)
     * 0~10: 정상, 11~20: 주의, 21~30: 경고, 31+: 위험
     */
    private String evaluateDailyMental(int totalScore) {
        if (totalScore <= 10) {
            return HealthSurveyRiskLevelDto.NORMAL;
        } else if (totalScore <= 20) {
            return HealthSurveyRiskLevelDto.CAUTION;
        } else if (totalScore <= 30) {
            return HealthSurveyRiskLevelDto.WARNING;
        } else {
            return HealthSurveyRiskLevelDto.DANGER;
        }
    }

    /**
     * 데일리 신체 (0~40)
     * 0~10: 정상, 11~20: 주의, 21~30: 경고, 31+: 위험
     */
    private String evaluateDailyPhysical(int totalScore) {
        if (totalScore <= 10) {
            return HealthSurveyRiskLevelDto.NORMAL;
        } else if (totalScore <= 20) {
            return HealthSurveyRiskLevelDto.CAUTION;
        } else if (totalScore <= 30) {
            return HealthSurveyRiskLevelDto.WARNING;
        } else {
            return HealthSurveyRiskLevelDto.DANGER;
        }
    }

    private HealthSurveyQuestionResponse toDto(HealthSurveyQuestion question) {
        return HealthSurveyQuestionResponse.builder()
            .healthSurveyQuestionNo(question.getHealthSurveyQuestionNo())
            .healthSurveyNo(
                question.getHealthSurvey() != null
                    ? question.getHealthSurvey().getHealthSurveyNo()
                    : null
            )
            .healthSurveyOrder(question.getHealthSurveyOrder())
            .healthSurveyQuestionContent(question.getHealthSurveyQuestionContent())
            .healthSurveyQuestionMinScore(question.getHealthSurveyQuestionMinScore())
            .healthSurveyQuestionMaxScore(question.getHealthSurveyQuestionMaxScore())
            .build();
    }
}


