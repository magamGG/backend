package com.kh.magamGG.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 칸반 카드 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanbanCardCreateRequest {
    private String title;
    private String description;
    private Long boardId;
    private Long projectMemberNo;  // PROJECT_MEMBER.PROJECT_MEMBER_NO (담당자) - 우선 사용
    private Long memberNo;         // 또는 MEMBER.MEMBER_NO - projectMemberNo 없을 때 프로젝트 내 해당 회원의 ProjectMember 조회
    private String startDate;      // yyyy-MM-dd
    private String dueDate;        // yyyy-MM-dd
}
