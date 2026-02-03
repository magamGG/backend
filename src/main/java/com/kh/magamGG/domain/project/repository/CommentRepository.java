package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** KANBAN_CARD_NO 기준 해당 카드의 노출 가능한 댓글 전부 조회 (생성일 오름차순) */
    List<Comment> findByKanbanCard_KanbanCardNoAndCommentStatusOrderByCommentCreatedAtAsc(Long kanbanCardNo, String commentStatus);
}
