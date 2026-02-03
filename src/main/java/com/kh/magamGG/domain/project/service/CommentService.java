package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.CommentCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.CommentResponse;
import com.kh.magamGG.domain.project.dto.response.DashboardFeedbackResponse;

import java.util.List;

public interface CommentService {

    List<CommentResponse> getCommentsByCardId(Long projectNo, Long cardId);

    CommentResponse createComment(Long projectNo, Long cardId, Long memberNo, CommentCreateRequest request);

    CommentResponse updateComment(Long projectNo, Long cardId, Long commentId, CommentUpdateRequest request);

    /** 작가 대시보드: 회원이 소속된 프로젝트의 칸반 카드에 달린 최신 코멘트 목록 */
    List<DashboardFeedbackResponse> getRecentFeedbackForMember(Long memberNo, int limit);
}
