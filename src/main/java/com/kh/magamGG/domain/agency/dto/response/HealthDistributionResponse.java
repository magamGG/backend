package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 - 건강 인원 분포 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthDistributionResponse {

    /** 건강 등급별 인원 [{ name, value, color }, ...] */
    private List<HealthItem> distribution;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthItem {
        private String name;
        private Long value;
        private String color;
    }
}
