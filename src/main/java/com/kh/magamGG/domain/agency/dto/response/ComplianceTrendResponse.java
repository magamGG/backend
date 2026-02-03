package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceTrendResponse {
    private List<TrendItem> trend;
    private Double monthOverMonthChange;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String month;
        private Number rate;
    }
}
