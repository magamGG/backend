package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 - 건강 인원 분포 응답 (정신/신체 분리, 미검진 포함, 에이전시 관리자 제외)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthDistributionResponse {

    /** 정신 건강(월간 정신) 등급별 인원: 위험, 주의, 경고, 정상, 미검진 */
    private List<HealthItem> mentalDistribution;
    /** 신체 건강(월간 신체) 등급별 인원: 위험, 주의, 경고, 정상, 미검진 */
    private List<HealthItem> physicalDistribution;

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
