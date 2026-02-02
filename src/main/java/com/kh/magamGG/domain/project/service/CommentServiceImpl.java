package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.CommentCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.CommentResponse;
import com.kh.magamGG.domain.project.entity.Comment;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.repository.CommentRepository;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.global.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final KanbanCardRepository kanbanCardRepository;

    @Override
    @Transactional
    public CommentResponse createComment(Long projectNo, Long cardId, CommentCreateRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("코멘트 내용을 입력해주세요.");
        }
        KanbanCard card = kanbanCardRepository.findById(cardId)
                .orElseThrow(() -> new ProjectNotFoundException("카드를 찾을 수 없습니다."));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        Comment comment = Comment.builder()
                .kanbanCard(card)
                .commentContent(request.getContent().trim())
                .commentStatus("active")
                .commentCreatedAt(LocalDateTime.now())
                .build();
        Comment saved = commentRepository.save(comment);
        return CommentResponse.builder()
                .id(saved.getCommentNo())
                .content(saved.getCommentContent())
                .commentCreatedAt(saved.getCommentCreatedAt() != null
                        ? saved.getCommentCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByCardId(Long projectNo, Long cardId) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
                .orElseThrow(() -> new ProjectNotFoundException("카드를 찾을 수 없습니다."));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        return commentRepository.findByKanbanCard_KanbanCardNoOrderByCommentNoAsc(cardId).stream()
                .filter(c -> "active".equalsIgnoreCase(c.getCommentStatus()))
                .map(c -> CommentResponse.builder()
                        .id(c.getCommentNo())
                        .content(c.getCommentContent())
                        .commentCreatedAt(c.getCommentCreatedAt() != null
                                ? c.getCommentCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long projectNo, Long cardId, Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ProjectNotFoundException("코멘트를 찾을 수 없습니다."));
        if (!comment.getKanbanCard().getKanbanCardNo().equals(cardId)) {
            throw new IllegalArgumentException("해당 카드의 코멘트가 아닙니다.");
        }
        if (!comment.getKanbanCard().getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 코멘트가 아닙니다.");
        }
        if (request.getStatus() != null && "block".equalsIgnoreCase(request.getStatus().trim())) {
            comment.setCommentStatus("block");
        } else if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            comment.setCommentContent(request.getContent().trim());
        } else if (request.getContent() != null && request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("코멘트 내용을 입력해주세요.");
        }
        Comment saved = commentRepository.save(comment);
        return CommentResponse.builder()
                .id(saved.getCommentNo())
                .content(saved.getCommentContent())
                .commentCreatedAt(saved.getCommentCreatedAt() != null
                        ? saved.getCommentCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : null)
                .build();
    }
}


