package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 - 평균 마감 준수율 추이 응답
 * GUIDE: AG-DS-04
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceTrendResponse {

    /** 월별 준수율 [{ month: "1월", rate: 78 }, ...] */
    private List<ComplianceMonthItem> trend;

    /** 전월 대비 변화율 (양수: 증가, 음수: 감소), null이면 표시 안함 */
    private Double monthOverMonthChange;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceMonthItem {
        private String month;
        private Double rate;
    }
}
