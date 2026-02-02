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
     * 특정 회원이 특정 설문에 대해 응답했는지 확인
     * (해당 설문의 문항들 중 하나라도 응답이 있으면 검진 완료로 간주)
     */
    @Query("SELECT i FROM HealthSurveyResponseItem i " +
           "WHERE i.member.memberNo = :memberNo " +
           "AND i.healthSurveyQuestion.healthSurvey.healthSurveyNo = :healthSurveyNo")
    List<HealthSurveyResponseItem> findByMemberNoAndHealthSurveyNo(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyNo") Long healthSurveyNo
    );
    
    /**
     * 특정 회원이 특정 설문 타입에 대해 응답했는지 확인
     */
    @Query("SELECT i FROM HealthSurveyResponseItem i " +
           "WHERE i.member.memberNo = :memberNo " +
           "AND i.healthSurveyQuestion.healthSurvey.healthSurveyType = :healthSurveyType")
    List<HealthSurveyResponseItem> findByMemberNoAndHealthSurveyType(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyType") String healthSurveyType
    );
    
    /**
     * 특정 회원의 특정 설문 응답 생성일 조회 (가장 최근 것)
     * 같은 설문의 모든 문항 응답은 동일한 CREATED_AT을 가지므로, 하나만 조회
     */
    @Query("SELECT i.healthSurveyQuestionItemCreatedAt FROM HealthSurveyResponseItem i " +
           "WHERE i.member.memberNo = :memberNo " +
           "AND i.healthSurveyQuestion.healthSurvey.healthSurveyNo = :healthSurveyNo " +
           "ORDER BY i.healthSurveyQuestionItemCreatedAt DESC")
    Optional<LocalDateTime> findLatestResponseDateByMemberNoAndHealthSurveyNo(
        @Param("memberNo") Long memberNo,
        @Param("healthSurveyNo") Long healthSurveyNo
    );
}

