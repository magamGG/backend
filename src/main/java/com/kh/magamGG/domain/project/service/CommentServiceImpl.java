package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.CommentCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.CommentResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.entity.Comment;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.repository.CommentRepository;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final MemberRepository memberRepository;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<CommentResponse> getCommentsByCardId(Long projectNo, Long cardId) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다: " + cardId));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        List<Comment> comments = commentRepository.findByKanbanCard_KanbanCardNoAndCommentStatusOrderByCommentCreatedAtAsc(cardId, "ACTIVE");
        return comments.stream().map(this::toCommentResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long projectNo, Long cardId, Long memberNo, CommentCreateRequest request) {
        if (memberNo == null) {
            throw new IllegalArgumentException("회원 번호(X-Member-No)가 필요합니다. 로그인 후 다시 시도해주세요.");
        }
        KanbanCard card = kanbanCardRepository.findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다: " + cardId));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
        Comment comment = new Comment();
        comment.setMember(member);
        comment.setKanbanCard(card);
        comment.setCommentContent(request.getContent() != null ? request.getContent() : "");
        comment.setCommentStatus("ACTIVE");
        comment.setCommentCreatedAt(java.time.LocalDateTime.now());
        if (comment.getMember() == null) {
            throw new IllegalStateException("댓글 작성자 정보가 설정되지 않았습니다. X-Member-No 헤더를 확인해주세요.");
        }
        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long projectNo, Long cardId, Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("코멘트를 찾을 수 없습니다: " + commentId));
        if (!comment.getKanbanCard().getKanbanCardNo().equals(cardId)) {
            throw new IllegalArgumentException("해당 카드의 코멘트가 아닙니다.");
        }
        if (!comment.getKanbanCard().getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        if (request.getContent() != null) comment.setCommentContent(request.getContent());
        if (request.getStatus() != null && "block".equalsIgnoreCase(request.getStatus())) {
            comment.setCommentStatus("BLOCKED");
        }
        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    private CommentResponse toCommentResponse(Comment c) {
        String createdAt = c.getCommentCreatedAt() != null ? c.getCommentCreatedAt().format(DATETIME_FMT) : null;
        Long authorMemberNo = c.getMember() != null ? c.getMember().getMemberNo() : null;
        String authorName = c.getMember() != null ? c.getMember().getMemberName() : null;
        return CommentResponse.builder()
            .id(c.getCommentNo())
            .authorMemberNo(authorMemberNo)
            .authorName(authorName)
            .content(c.getCommentContent())
            .commentCreatedAt(createdAt)
            .build();
    }
}
