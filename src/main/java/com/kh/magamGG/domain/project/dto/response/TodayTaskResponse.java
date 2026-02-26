package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아티스트 대시보드 "오늘 할 일"용 DTO.
 * 담당자인 칸반 카드 중 마감일이 오늘이고 미완료(N)인 카드만 노출.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayTaskResponse {
    private Long id;
    private Long projectNo;
    private String projectName;
    private String projectColor;
    private Long boardId;
    private String title;
    private String description;
    /** 시작일 (yyyy-MM-dd). 시작일 기준 마감일까지 일수 계산용 */
    private String startDate;
    private String dueDate;
    /** 완료 여부 (칸반 카드 상태 Y면 true). 다른 페이지 갔다 와도 완료된 일이 목록에 남도록 */
    private Boolean completed;
}
