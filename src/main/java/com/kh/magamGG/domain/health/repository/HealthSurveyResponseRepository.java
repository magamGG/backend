package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthSurveyResponseRepository extends JpaRepository<HealthSurveyResponse, Long> {
    
    /**
     * 특정 회원이 특정 설문에 대해 응답했는지 확인
     */
    List<HealthSurveyResponse> findByMember_MemberNoAndHealthSurvey_HealthSurveyNo(
        Long memberNo,
        Long healthSurveyNo
    );
    
    /**
     * 특정 회원의 특정 설문 최근 응답 조회
     */
    @Query("SELECT r FROM HealthSurveyResponse r " +
           "WHERE r.member.memberNo = :memberNo " +
           "AND r.healthSurvey.healthSurveyNo = :healthSurveyNo " +
           "ORDER BY r.healthSurveyResponseCreatedAt DESC")
    Optional<HealthSurveyResponse> findLatestByMemberNoAndHealthSurveyNo(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyNo") Long healthSurveyNo
    );
    
    /**
     * 특정 회원의 특정 설문 응답 생성일 조회 (가장 최근 것)
     */
    @Query("SELECT r.healthSurveyResponseCreatedAt FROM HealthSurveyResponse r " +
           "WHERE r.member.memberNo = :memberNo " +
           "AND r.healthSurvey.healthSurveyNo = :healthSurveyNo " +
           "ORDER BY r.healthSurveyResponseCreatedAt DESC")
    Optional<LocalDateTime> findLatestResponseDateByMemberNoAndHealthSurveyNo(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyNo") Long healthSurveyNo
    );
}
