package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthSurveyQuestionRepository extends JpaRepository<HealthSurveyQuestion, Long> {

    /**
     * 설문 번호로 해당 설문의 모든 문항 조회 (문항 순서 기준 정렬)
     */
    List<HealthSurveyQuestion> findByHealthSurvey_HealthSurveyNoOrderByHealthSurveyOrderAsc(Long healthSurveyNo);

    /**
     * HEALTH_SURVEY_QUESTION_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 문항 목록 조회
     */
    List<HealthSurveyQuestion> findByHealthSurveyQuestionTypeOrderByHealthSurveyOrderAsc(String healthSurveyType);
    
    /**
     * AgencyNo와 HEALTH_SURVEY_QUESTION_TYPE으로 문항 목록 조회
     * 해당 에이전시의 설문 중 특정 타입의 질문만 조회
     */
    List<HealthSurveyQuestion> findByHealthSurvey_Agency_AgencyNoAndHealthSurveyQuestionTypeOrderByHealthSurveyOrderAsc(
        Long agencyNo, 
        String healthSurveyType
    );
}

