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
}