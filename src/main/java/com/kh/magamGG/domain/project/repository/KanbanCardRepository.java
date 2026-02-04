package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.KanbanCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    @Query("SELECT kc FROM KanbanCard kc " +
           "JOIN kc.kanbanBoard kb " +
           "WHERE kb.project.projectNo = :projectNo")
    List<KanbanCard> findByProjectNo(@Param("projectNo") Long projectNo);

    List<KanbanCard> findByKanbanBoard_Project_ProjectNo(Long projectNo);

    /**
     * 담당자 배정 + 미완료(N) 카드 전부 조회, 마감일 가까운 순 정렬 (아티스트 대시보드 오늘 할 일용).
     * Y(완료), D(삭제) 제외. 마감일 NULL은 맨 뒤로.
     */
    @Query("SELECT kc FROM KanbanCard kc " +
           "WHERE kc.projectMember.member.memberNo = :memberNo AND kc.kanbanCardStatus = :status " +
           "ORDER BY kc.kanbanCardEndedAt ASC NULLS LAST")
    List<KanbanCard> findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(
            @Param("memberNo") Long memberNo, @Param("status") String status);

    /**
     * 회원에게 배정된 칸반 카드 수 (PROJECT_MEMBER 경유, 상태 N=미완료만)
     */
    @Query("SELECT COUNT(kc) FROM KanbanCard kc " +
           "WHERE kc.projectMember.member.memberNo = :memberNo AND kc.kanbanCardStatus = 'N'")
    long countByProjectMember_Member_MemberNo(@Param("memberNo") Long memberNo);

    /**
     * 회원에게 배정된 칸반 카드 수 - 지정 상태만 (PROJECT_MEMBER 경유, status='Y' 완료 등)
     */
    @Query("SELECT COUNT(kc) FROM KanbanCard kc " +
           "WHERE kc.projectMember.member.memberNo = :memberNo AND kc.kanbanCardStatus = :status")
    long countByProjectMember_Member_MemberNoAndKanbanCardStatus(
            @Param("memberNo") Long memberNo, @Param("status") String status);

    /**
     * 회원에게 배정된 칸반 카드 수 - STATUS가 'D'(삭제)가 아닌 것만 (카드 "작업 N개" 표시용)
     */
    @Query("SELECT COUNT(kc) FROM KanbanCard kc " +
           "WHERE kc.projectMember.member.memberNo = :memberNo AND (kc.kanbanCardStatus IS NULL OR kc.kanbanCardStatus <> 'D')")
    long countByProjectMember_Member_MemberNoAndKanbanCardStatusNotD(@Param("memberNo") Long memberNo);
}

