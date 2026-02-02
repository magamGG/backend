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
     * (총점이 저장된 레코드가 있으면 검진 완료로 간주)
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
     * CREATED_AT의 유무로 검진 완료 여부 판단
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

