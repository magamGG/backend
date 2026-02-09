package com.kh.magamGG.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 칸반 카드 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanbanCardUpdateRequest {
    private String title;
    private String description;
    private Long boardId;          // 보드 이동 시
    private Long projectMemberNo;
    private Long memberNo;
    private String startDate;
    private String dueDate;
    private Boolean completed;     // true면 Y(체크), false면 N(언체크)
    private String status;        // "D"면 삭제(숨김), 없으면 completed로 Y/N 처리
}
