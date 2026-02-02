package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * PROJECT_MEMBER에 포함된 회원의 프로젝트 목록 조회
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.projectMembers pm WHERE pm.member.memberNo = :memberNo")
    List<Project> findByProjectMember_MemberNo(@Param("memberNo") Long memberNo);

    /**
     * 해당 에이전시 소속 회원이 참여한 모든 프로젝트 조회
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.projectMembers pm WHERE pm.member.agency.agencyNo = :agencyNo")
    List<Project> findAllByAgencyNo(@Param("agencyNo") Long agencyNo);
}
