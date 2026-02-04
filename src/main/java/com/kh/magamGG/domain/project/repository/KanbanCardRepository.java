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

    /**
     * 여러 회원의 특정 기간 내 마감 칸반 카드 조회 (담당자/에이전시 대시보드 마감 임박 현황용)
     * memberNo IN (...) AND kanbanCardEndedAt BETWEEN :fromDate AND :toDate
     * 완료(Y), 삭제(D) 제외
     */
    @Query("SELECT kc FROM KanbanCard kc " +
           "JOIN FETCH kc.projectMember pm " +
           "JOIN FETCH pm.member m " +
           "WHERE m.memberNo IN :memberNos " +
           "AND kc.kanbanCardEndedAt >= :fromDate " +
           "AND kc.kanbanCardEndedAt <= :toDate " +
           "AND kc.kanbanCardStatus != 'Y' " +
           "AND kc.kanbanCardStatus != 'D' " +
           "ORDER BY kc.kanbanCardEndedAt ASC")
    List<KanbanCard> findByMemberNosAndDateRange(
            @Param("memberNos") List<Long> memberNos,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate);
}

