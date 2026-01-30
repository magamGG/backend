package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthSurveyRepository extends JpaRepository<HealthSurvey, Long> {

    /**
     * 설문 이름(예: PHQ-9, GAD, QuickDASH 등)으로 단일 설문 조회
     */
    Optional<HealthSurvey> findByHealthSurveyName(String healthSurveyName);

    /**
     * HEALTH_SURVEY_TYPE(데일리 정신 / 데일리 신체 / 월간 정신 / 월간 신체)으로 설문 조회
     */
    Optional<HealthSurvey> findByHealthSurveyType(String healthSurveyType);
}

