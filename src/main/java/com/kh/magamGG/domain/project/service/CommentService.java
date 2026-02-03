package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.CommentCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    List<CommentResponse> getCommentsByCardId(Long projectNo, Long cardId);

    CommentResponse createComment(Long projectNo, Long cardId, CommentCreateRequest request);

    CommentResponse updateComment(Long projectNo, Long cardId, Long commentId, CommentUpdateRequest request);
}
