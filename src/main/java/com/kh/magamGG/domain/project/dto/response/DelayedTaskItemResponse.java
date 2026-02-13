package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 담당자 퀵 리포트용: 지연된 작업 한 건 (작업명, 작가명, 지연 일수)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayedTaskItemResponse {
    private String title;
    private String artistName;
    /** 지연 일수 (마감일 기준 오늘까지) */
    private int daysDelayed;
}
