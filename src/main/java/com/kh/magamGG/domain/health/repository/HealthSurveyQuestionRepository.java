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
}

