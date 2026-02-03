package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** KANBAN_CARD_NO 기준 해당 카드의 노출 가능한 댓글 전부 조회 (생성일 오름차순) */
    List<Comment> findByKanbanCard_KanbanCardNoAndCommentStatusOrderByCommentCreatedAtAsc(Long kanbanCardNo, String commentStatus);

    /** KANBAN_CARD_NO 기준 해당 카드의 전체 댓글 조회 (생성일 오름차순) */
    List<Comment> findByKanbanCard_KanbanCardNoOrderByCommentCreatedAtAsc(Long kanbanCardNo);

    /** 회원이 소속된 프로젝트의 칸반 카드에 달린 최신 코멘트 조회 (작가 대시보드 피드백용) */
    @Query("SELECT c FROM Comment c " +
           "JOIN c.kanbanCard kc " +
           "JOIN kc.kanbanBoard kb " +
           "JOIN kb.project p " +
           "WHERE p.projectNo IN :projectNos " +
           "ORDER BY c.commentCreatedAt DESC")
    List<Comment> findRecentByProjectNos(@Param("projectNos") List<Long> projectNos, Pageable pageable);

    /**
     * 담당자인 카드에 다른 사람이 작성한 최신 코멘트만 조회 (아티스트 대시보드 피드백용).
     * - 카드 담당자(projectMember.member) = 요청 회원
     * - 코멘트 작성자 != 요청 회원 (본인 코멘트 제외)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN c.kanbanCard kc " +
           "JOIN kc.projectMember pm " +
           "JOIN pm.member assignee " +
           "JOIN kc.kanbanBoard kb " +
           "JOIN kb.project p " +
           "WHERE p.projectNo IN :projectNos " +
           "AND assignee.memberNo = :memberNo " +
           "AND c.member.memberNo <> :memberNo " +
           "AND c.commentStatus = 'ACTIVE' " +
           "ORDER BY c.commentCreatedAt DESC")
    List<Comment> findRecentFeedbackForAssignee(@Param("projectNos") List<Long> projectNos, @Param("memberNo") Long memberNo, Pageable pageable);
}
