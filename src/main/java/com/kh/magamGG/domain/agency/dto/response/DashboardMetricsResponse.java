package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {
    private Integer averageDeadlineComplianceRate;
    private Integer activeArtistCount;
    private Integer activeProjectCount;
    private String complianceRateChange;
    private String activeArtistChange;
    private String activeProjectChange;
}
