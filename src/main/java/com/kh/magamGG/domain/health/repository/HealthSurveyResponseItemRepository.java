package com.kh.magamGG.domain.health.repository;

import com.kh.magamGG.domain.health.entity.HealthSurveyResponseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthSurveyResponseItemRepository extends JpaRepository<HealthSurveyResponseItem, Long> {
    
    /**
     * 특정 회원이 특정 HEALTH_SURVEY_QUESTION_TYPE에 대해 응답했는지 확인
     * (설문 타입은 HEALTH_SURVEY_QUESTION_TYPE 기준)
     */
    @Query("SELECT i FROM HealthSurveyResponseItem i " +
           "WHERE i.member.memberNo = :memberNo " +
           "AND i.healthSurveyQuestion.healthSurveyQuestionType = :healthSurveyQuestionType")
    List<HealthSurveyResponseItem> findByMemberNoAndHealthSurveyType(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyQuestionType") String healthSurveyQuestionType
    );
    
    /**
     * 에이전시 소속 회원들의 건강 설문 응답 항목 조회 (최신 응답 산출용)
     * HealthSurvey 연관관계 제거로 인해 쿼리 수정
     */
    @Query("SELECT i FROM HealthSurveyResponseItem i " +
           "JOIN FETCH i.healthSurveyQuestion q " +
           "JOIN i.member m " +
           "WHERE m.agency.agencyNo = :agencyNo " +
           "ORDER BY i.healthSurveyQuestionItemCreatedAt DESC")
    List<HealthSurveyResponseItem> findByAgencyNoWithSurvey(@Param("agencyNo") Long agencyNo);

    @Query("SELECT i FROM HealthSurveyResponseItem i " +
           "JOIN FETCH i.healthSurveyQuestion q " +
           "WHERE i.member.memberNo = :memberNo " +
           "AND q.healthSurveyQuestionType = :type " +
           "AND q.healthSurveyOrder BETWEEN :minOrder AND :maxOrder")
    List<HealthSurveyResponseItem> findByMemberNoAndTypeAndOrderRange(
        @Param("memberNo") Long memberNo,
        @Param("type") String type,
        @Param("minOrder") int minOrder,
        @Param("maxOrder") int maxOrder);
}

