package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아티스트 캘린더용 칸반 카드 DTO.
 * 담당자 배정 카드 중 해당 월과 기간이 겹치는 카드 (KANBAN_CARD_STARTED_AT, ENDED_AT, PROJECT_COLOR).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarCardResponse {
    private Long id;
    private String title;
    /** 기간 시작일 (ISO: yyyy-MM-dd) */
    private String startDate;
    /** 기간 종료일 (ISO: yyyy-MM-dd) */
    private String endDate;
    private String projectColor;
    private String projectName;
    private Long projectNo;
}
