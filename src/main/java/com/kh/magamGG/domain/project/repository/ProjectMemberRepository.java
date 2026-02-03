package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByMember_MemberNo(Long memberNo);

    List<ProjectMember> findByProject_ProjectNo(Long projectNo);

    boolean existsByProject_ProjectNoAndMember_MemberNo(Long projectNo, Long memberNo);

    Optional<ProjectMember> findFirstByProject_ProjectNoAndProjectMemberRole(Long projectNo, String projectMemberRole);

    /** 에이전시 소속 회원이 포함된 프로젝트 번호 목록 (중복 제거) */
    @Query("SELECT DISTINCT pm.project.projectNo FROM ProjectMember pm WHERE pm.member.agency.agencyNo = :agencyNo")
    List<Long> findDistinctProjectNosByMember_Agency_AgencyNo(@Param("agencyNo") Long agencyNo);

    /**
     * 프로젝트별 에이전시 소속 작가(웹툰/웹소설) 수
     */
    @Query("SELECT COUNT(DISTINCT pm.projectMemberNo) FROM ProjectMember pm " +
           "WHERE pm.project.projectNo = :projectNo " +
           "AND pm.member.agency.agencyNo = :agencyNo " +
           "AND (pm.member.memberRole = '웹툰 작가' OR pm.member.memberRole = '웹소설 작가')")
    long countArtistsByProjectAndAgency(@Param("projectNo") Long projectNo, @Param("agencyNo") Long agencyNo);
}