package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByKanbanCard_KanbanCardNoOrderByCommentNoAsc(Long kanbanCardNo);
}


