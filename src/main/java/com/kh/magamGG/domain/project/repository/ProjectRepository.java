package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 에이전시 소속 회원이 참여한 진행 중(연재) 프로젝트 목록
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "JOIN p.projectMembers pm " +
           "WHERE pm.member.agency.agencyNo = :agencyNo " +
           "AND p.projectStatus = '연재'")
    List<Project> findActiveProjectsByAgencyNo(@Param("agencyNo") Long agencyNo);

    /**
     * 에이전시 소속 회원이 참여한 모든 프로젝트 (마감 준수율 계산용)
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "JOIN p.projectMembers pm " +
           "WHERE pm.member.agency.agencyNo = :agencyNo")
    List<Project> findAllProjectsByAgencyNo(@Param("agencyNo") Long agencyNo);

    /**
     * 해당 에이전시에 동일한 프로젝트명을 가진 프로젝트 수 (대소문자·앞뒤 공백 무시)
     */
    @Query("SELECT COUNT(p) FROM Project p " +
           "JOIN p.projectMembers pm " +
           "WHERE pm.member.agency.agencyNo = :agencyNo " +
           "AND LOWER(TRIM(p.projectName)) = LOWER(TRIM(:projectName))")
    long countByProjectNameAndAgencyNo(
            @Param("projectName") String projectName,
            @Param("agencyNo") Long agencyNo);

    /**
     * 특정 회원(작가)이 참여한 진행 중('연재') 프로젝트 목록
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "JOIN p.projectMembers pm " +
           "WHERE pm.member.memberNo = :memberNo " +
           "AND p.projectStatus = '연재'")
    List<Project> findActiveProjectsByMemberNo(@Param("memberNo") Long memberNo);
    /** 에이전시 소속 회원이 참여한 프로젝트 중 projectStartedAt 기준 해당 시점까지 생성된 수 (전월 대비 집계용) */
    @Query("SELECT COUNT(DISTINCT p) FROM Project p " +
           "JOIN p.projectMembers pm " +
           "WHERE pm.member.agency.agencyNo = :agencyNo " +
           "AND p.projectStartedAt IS NOT NULL AND p.projectStartedAt <= :before")
    long countByAgencyNoAndProjectStartedAtBefore(
            @Param("agencyNo") Long agencyNo,
            @Param("before") LocalDateTime before);
}
