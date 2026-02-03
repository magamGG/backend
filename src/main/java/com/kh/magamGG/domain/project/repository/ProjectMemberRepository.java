package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByMember_MemberNo(Long memberNo);

    /**
     * 프로젝트별 에이전시 소속 작가(웹툰/웹소설) 수
     */
    @Query("SELECT COUNT(DISTINCT pm.projectMemberNo) FROM ProjectMember pm " +
           "WHERE pm.project.projectNo = :projectNo " +
           "AND pm.member.agency.agencyNo = :agencyNo " +
           "AND (pm.member.memberRole = '웹툰 작가' OR pm.member.memberRole = '웹소설 작가')")
    long countArtistsByProjectAndAgency(@Param("projectNo") Long projectNo, @Param("agencyNo") Long agencyNo);
}