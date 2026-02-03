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
public class AttendanceDistributionResponse {
    private List<DistributionItem> distribution;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionItem {
        private String name;
        private Integer value;
        private String color;
    }
}
