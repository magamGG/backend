package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthSurveyQuestionRepository extends JpaRepository<HealthSurveyQuestion, Long> {

    /**
     * HEALTH_SURVEY_QUESTION_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 문항 목록 조회
     * 소속 상관없이 모든 문항 조회
     */
    List<HealthSurveyQuestion> findByHealthSurveyQuestionTypeOrderByHealthSurveyOrderAsc(String healthSurveyType);
}

