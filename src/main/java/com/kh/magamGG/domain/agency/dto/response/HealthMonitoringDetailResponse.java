package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 검진 모니터링 상세 목록 응답 (정신/신체 타입별 회원별 점수·상태·최근 검사일)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthMonitoringDetailResponse {

    /** 정신 또는 신체 타입: "mental" | "physical" */
    private String type;
    private List<HealthMonitoringDetailItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthMonitoringDetailItem {
        private Long memberNo;
        private String memberName;
        private String position;
        private Integer totalScore;
        private String status;
        private String lastCheckDate;
    }
}
