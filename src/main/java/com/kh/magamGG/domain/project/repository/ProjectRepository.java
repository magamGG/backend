package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 프로젝트별 에이전시 소속 작가(웹툰/웹소설) 수
     */
    @Query("SELECT COUNT(DISTINCT pm.projectMemberNo) FROM ProjectMember pm " +
           "WHERE pm.project.projectNo = :projectNo " +
           "AND pm.member.agency.agencyNo = :agencyNo " +
           "AND (pm.member.memberRole = '웹툰 작가' OR pm.member.memberRole = '웹소설 작가')")
    long countArtistsByProjectAndAgency(@Param("projectNo") Long projectNo, @Param("agencyNo") Long agencyNo);

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
}
