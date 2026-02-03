package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByKanbanCard_KanbanCardNoOrderByCommentCreatedAtAsc(Long kanbanCardNo);

    /** 회원이 소속된 프로젝트의 칸반 카드에 달린 최신 코멘트 조회 (작가 대시보드 피드백용) */
    @Query("SELECT c FROM Comment c " +
           "JOIN c.kanbanCard kc " +
           "JOIN kc.kanbanBoard kb " +
           "JOIN kb.project p " +
           "WHERE p.projectNo IN :projectNos " +
           "ORDER BY c.commentCreatedAt DESC")
    List<Comment> findRecentByProjectNos(@Param("projectNos") List<Long> projectNos, Pageable pageable);
}
