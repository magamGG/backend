package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작가 대시보드 피드백(프로젝트 칸반 카드 코멘트) 목록용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFeedbackResponse {
    private Long commentId;
    private Long projectNo;
    private String projectName;
    private String projectColor;
    private Long cardId;
    private String cardTitle;
    private String content;
    private String writerName;
    private String commentCreatedAt;
}
