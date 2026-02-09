package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthSurveyRepository extends JpaRepository<HealthSurvey, Long> {

    /**
     * 에이전시별 HEALTH_SURVEY 조회 (에이전시당 1건 가정)
     */
    Optional<HealthSurvey> findByAgency_AgencyNo(Long agencyNo);
}

