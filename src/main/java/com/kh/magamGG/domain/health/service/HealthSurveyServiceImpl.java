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
    public List<HealthSurveyQuestionResponse> getQuestionsByAgencyNo(Long agencyNo) {
        HealthSurvey survey = healthSurveyRepository.findByAgency_AgencyNo(agencyNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 에이전시의 설문을 찾을 수 없습니다: agencyNo=" + agencyNo));
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

        // 5. 총점/위험도 등급 계산 (타입 없이 총점만으로 등급 산정)
        String riskLevel = evaluateRiskLevelByScore(totalScore);

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
        // 타입 제거 후 총점만으로 등급 산정
        return evaluateRiskLevelByScore(totalScore);
    }

    /**
     * 총점만으로 위험도 등급 산정 (0~20 정상, 21~40 주의, 41~60 경고, 61+ 위험)
     */
    private String evaluateRiskLevelByScore(int totalScore) {
        if (totalScore <= 20) {
            return HealthSurveyRiskLevelDto.NORMAL;
        } else if (totalScore <= 40) {
            return HealthSurveyRiskLevelDto.CAUTION;
        } else if (totalScore <= 60) {
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