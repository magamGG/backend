package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 - 금일 출석 현황 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDistributionResponse {

    /** 출석 유형별 인원 [{ name, value, color }, ...] */
    private List<AttendanceItem> distribution;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceItem {
        private String name;
        private Long value;
        private String color;
    }
}
