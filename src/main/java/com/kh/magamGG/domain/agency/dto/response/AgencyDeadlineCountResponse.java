package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 마감 임박 현황 (오늘~4일 후, 담당자 관리 프로젝트의 업무 기준)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDeadlineCountResponse {

    private List<DeadlineItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeadlineItem {
        private String name;  // "오늘", "내일", "2일 후", "3일 후", "4일 후"
        private int count;
    }
}
