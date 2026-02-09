package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.dto.response.HealthSurveyQuestionResponse;
import com.kh.magamGG.domain.health.dto.HealthSurveyRiskLevelDto;
import com.kh.magamGG.domain.health.dto.request.HealthSurveyAnswerRequest;
import com.kh.magamGG.domain.health.dto.request.HealthSurveySubmitRequest;
import com.kh.magamGG.domain.health.dto.response.HealthSurveySubmitResponse;
import com.kh.magamGG.domain.health.dto.response.HealthSurveyResponseStatusResponse;
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
import java.util.Map;
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
    public List<HealthSurveyQuestionResponse> getQuestionsByType(String healthSurveyType) {
        // 소속 상관없이 타입으로만 질문 조회
        List<HealthSurveyQuestion> questions =
            healthSurveyQuestionRepository.findByHealthSurveyQuestionTypeOrderByHealthSurveyOrderAsc(healthSurveyType);

        return questions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 건강 설문 응답 제출
     * - 문항별 점수를 각각 HEALTH_SURVEY_RESPONSE_ITEM에 저장 (항목별 체크, 상세보기 확장용)
     * - 같은 제출은 HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT로 묶음
     * - 총점은 저장하지 않고 조회 시 합산하여 사용
     */
    @Override
    @Transactional
    public HealthSurveySubmitResponse submitSurveyResponse(HealthSurveySubmitRequest request) {

        // 1. 회원 조회
        Member member = memberRepository.findById(request.getMemberNo())
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + request.getMemberNo()));

        List<HealthSurveyAnswerRequest> answers = request.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("응답 항목이 없습니다.");
        }

        // 2. 동일 설문 타입 검증 및 문항별 저장
        LocalDateTime responseCreatedAt = LocalDateTime.now();
        String surveyType = null;
        int totalScore = 0;

        for (HealthSurveyAnswerRequest answer : answers) {
            if (answer.getQuestionId() == null) {
                throw new IllegalArgumentException("질문 ID가 없습니다.");
            }
            HealthSurveyQuestion question = healthSurveyQuestionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + answer.getQuestionId()));
            if (surveyType == null) {
                surveyType = question.getHealthSurveyQuestionType();
            } else if (!surveyType.equals(question.getHealthSurveyQuestionType())) {
                throw new IllegalArgumentException("서로 다른 설문 타입의 문항을 함께 제출할 수 없습니다.");
            }
            int score = answer.getScore() != null ? answer.getScore() : 0;
            if (score < 0) {
                throw new IllegalArgumentException("문항 점수는 0 이상이어야 합니다.");
            }
            totalScore += score;

            HealthSurveyResponseItem item = new HealthSurveyResponseItem();
            item.setMember(member);
            item.setHealthSurveyQuestion(question);
            item.setHealthSurveyQuestionItemAnswerScore(score);
            item.setHealthSurveyQuestionItemCreatedAt(responseCreatedAt);
            healthSurveyResponseItemRepository.save(item);
        }

        return HealthSurveySubmitResponse.builder()
            .memberNo(member.getMemberNo())
            .totalScore(totalScore)
            .riskLevel(evaluateRiskLevel(surveyType, totalScore))
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

    @Override
    public HealthSurveyResponseStatusResponse getSurveyResponseStatus(Long memberNo, String healthSurveyQuestionType) {
        // 1. 회원 정보 조회하여 AgencyNo 가져오기 (HealthSurvey 조회용)
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
        
        Long agencyNo = member.getAgency().getAgencyNo();
        
        // 2. 타입으로 질문 조회 (소속 상관없이)
        List<HealthSurveyQuestion> questions = 
            healthSurveyQuestionRepository.findByHealthSurveyQuestionTypeOrderByHealthSurveyOrderAsc(healthSurveyQuestionType);
        
        if (questions == null || questions.isEmpty()) {
            return HealthSurveyResponseStatusResponse.builder()
                .isCompleted(false)
                .lastCheckDate(null)
                .totalScore(null)
                .riskLevel(null)
                .healthSurveyPeriod(15)  // 기본값
                .healthSurveyCycle(30)   // 기본값
                .nextCheckupDate(null)
                .daysRemaining(null)
                .deadlineDate(null)
                .build();
        }
        
        // 3. HealthSurvey를 AgencyNo로 직접 조회 (period, cycle 정보용)
        HealthSurvey healthSurvey = healthSurveyRepository.findByAgency_AgencyNo(agencyNo)
            .orElse(null);
        
        // 4. INT형 필드 처리 (null 체크 및 기본값 설정)
        Integer period = healthSurvey != null && healthSurvey.getHealthSurveyPeriod() != null 
            ? healthSurvey.getHealthSurveyPeriod() : 15;  // INT → Integer
        Integer cycle = healthSurvey != null && healthSurvey.getHealthSurveyCycle() != null 
            ? healthSurvey.getHealthSurveyCycle() : 30;   // INT → Integer
        
        // 4. 회원의 해당 타입에 대한 응답 조회 (문항별 row)
        List<HealthSurveyResponseItem> responseItems =
            healthSurveyResponseItemRepository.findByMemberNoAndHealthSurveyType(memberNo, healthSurveyQuestionType);

        // 5. 응답이 없으면 미완료 상태 반환
        if (responseItems == null || responseItems.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDate = now.plusDays(period);
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, deadlineDate);
            return HealthSurveyResponseStatusResponse.builder()
                .isCompleted(false)
                .lastCheckDate(null)
                .totalScore(null)
                .riskLevel(null)
                .healthSurveyPeriod(period)
                .healthSurveyCycle(cycle)
                .nextCheckupDate(null)
                .daysRemaining((int) daysRemaining)
                .deadlineDate(deadlineDate)
                .build();
        }

        // 6. CREATED_AT 기준으로 제출 그룹 묶기, 그 중 최신 제출만 사용
        Map<LocalDateTime, List<HealthSurveyResponseItem>> byCreatedAt = responseItems.stream()
            .filter(item -> item.getHealthSurveyQuestionItemCreatedAt() != null)
            .collect(Collectors.groupingBy(HealthSurveyResponseItem::getHealthSurveyQuestionItemCreatedAt));

        LocalDateTime latestCreatedAt = byCreatedAt.keySet().stream()
            .max(LocalDateTime::compareTo)
            .orElse(null);
        if (latestCreatedAt == null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDate = now.plusDays(period);
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, deadlineDate);
            return HealthSurveyResponseStatusResponse.builder()
                .isCompleted(false)
                .lastCheckDate(null)
                .totalScore(null)
                .riskLevel(null)
                .healthSurveyPeriod(period)
                .healthSurveyCycle(cycle)
                .nextCheckupDate(null)
                .daysRemaining((int) daysRemaining)
                .deadlineDate(deadlineDate)
                .build();
        }

        // 7. 최신 제출의 문항별 점수 합산 → 총점, 위험도 계산
        List<HealthSurveyResponseItem> latestSubmission = byCreatedAt.get(latestCreatedAt);
        int totalScore = latestSubmission.stream()
            .mapToInt(item -> item.getHealthSurveyQuestionItemAnswerScore() != null ? item.getHealthSurveyQuestionItemAnswerScore() : 0)
            .sum();
        String riskLevel = evaluateRiskLevel(healthSurveyQuestionType, totalScore);

        LocalDateTime lastCheckDate = latestCreatedAt;
        LocalDateTime nextCheckupDate = lastCheckDate.plusDays(cycle);
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, nextCheckupDate);

        return HealthSurveyResponseStatusResponse.builder()
            .isCompleted(true)
            .lastCheckDate(lastCheckDate)
            .totalScore(totalScore)
            .riskLevel(riskLevel)
            .healthSurveyPeriod(period)
            .healthSurveyCycle(cycle)
            .nextCheckupDate(nextCheckupDate)
            .daysRemaining((int) daysRemaining)
            .deadlineDate(null)
            .build();
    }

    private HealthSurveyQuestionResponse toDto(HealthSurveyQuestion question) {
        return HealthSurveyQuestionResponse.builder()
            .healthSurveyQuestionNo(question.getHealthSurveyQuestionNo())
            .healthSurveyOrder(question.getHealthSurveyOrder())
            .healthSurveyQuestionContent(question.getHealthSurveyQuestionContent())
            .healthSurveyQuestionType(question.getHealthSurveyQuestionType())
            .healthSurveyQuestionMinScore(question.getHealthSurveyQuestionMinScore())
            .healthSurveyQuestionMaxScore(question.getHealthSurveyQuestionMaxScore())
            .build();
    }
}


