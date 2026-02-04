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
     * - 프론트엔드에서 각 문항별 점수의 총합을 계산하여 전송
     * - 백엔드에서는 총점만 받아서 HEALTH_SURVEY_QUESTION_ITEM_ANSWER_SCORE에 저장
     * - HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT의 유무로 검진 완료 여부 판단
     */
    @Override
    @Transactional
    public HealthSurveySubmitResponse submitSurveyResponse(HealthSurveySubmitRequest request) {

        // 1. 회원 조회
        Member member = memberRepository.findById(request.getMemberNo())
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + request.getMemberNo()));

        // 2. 총점 계산 (answers의 각 문항 점수 합산)
        List<HealthSurveyAnswerRequest> answers = request.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("응답 항목이 없습니다.");
        }
        int totalScore = answers.stream()
            .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
            .sum();
        if (totalScore < 0) {
            throw new IllegalArgumentException("총점이 유효하지 않습니다: " + totalScore);
        }

        // 3. 첫 번째 답변의 questionId로 질문 조회하여 타입 확인
        Long firstQuestionId = answers.get(0).getQuestionId();
        HealthSurveyQuestion firstQuestion = healthSurveyQuestionRepository.findById(firstQuestionId)
            .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + firstQuestionId));
        
        String surveyType = firstQuestion.getHealthSurveyQuestionType();

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

        // 5. 클라이언트로 반환
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
        
        // 4. 회원의 해당 HEALTH_SURVEY_QUESTION_TYPE에 대한 응답 조회
        List<HealthSurveyResponseItem> responseItems = 
            healthSurveyResponseItemRepository.findByMemberNoAndHealthSurveyType(memberNo, healthSurveyQuestionType);

        // 5. 응답이 없으면 미완료 상태 반환 (기간 계산 포함)
        if (responseItems == null || responseItems.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDate = now.plusDays(period);  // 검사 기간만큼 마감일 설정
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

        // 6. 가장 최근 응답 찾기 (CREATED_AT 기준, CREATED_AT가 null이 아닌 것만)
        // HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT가 존재하면 검사 완료로 간주
        HealthSurveyResponseItem latestResponse = responseItems.stream()
            .filter(item -> item.getHealthSurveyQuestionItemCreatedAt() != null)  // CREATED_AT가 null이 아닌 것만 필터링
            .max((a, b) -> a.getHealthSurveyQuestionItemCreatedAt()
                .compareTo(b.getHealthSurveyQuestionItemCreatedAt()))
            .orElse(null);

        // 7. CREATED_AT가 존재하는 응답이 없으면 미완료 상태 반환 (기간 계산 포함)
        if (latestResponse == null || latestResponse.getHealthSurveyQuestionItemCreatedAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDate = now.plusDays(period);  // 검사 기간만큼 마감일 설정
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

        // 8. 총점과 위험도 계산
        Integer totalScore = latestResponse.getHealthSurveyQuestionItemAnswerScore();
        String riskLevel = evaluateRiskLevel(healthSurveyQuestionType, totalScore != null ? totalScore : 0);
        
        // 9. 날짜 계산 (검진 완료 상태)
        LocalDateTime lastCheckDate = latestResponse.getHealthSurveyQuestionItemCreatedAt();
        LocalDateTime nextCheckupDate = lastCheckDate.plusDays(cycle);  // 검사 주기만큼 다음 검진일 계산
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, nextCheckupDate);

        // 10. 응답 반환 (CREATED_AT가 존재하므로 완료 상태)
        return HealthSurveyResponseStatusResponse.builder()
            .isCompleted(true)  // HEALTH_SURVEY_QUESTION_ITEM_CREATED_AT가 존재하므로 완료
            .lastCheckDate(lastCheckDate)
            .totalScore(totalScore)
            .riskLevel(riskLevel)
            .healthSurveyPeriod(period)
            .healthSurveyCycle(cycle)
            .nextCheckupDate(nextCheckupDate)
            .daysRemaining((int) daysRemaining)
            .deadlineDate(null)  // 완료 상태이므로 마감일 없음
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


