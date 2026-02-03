package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에이전시 대시보드 메트릭 응답 DTO
 * GUIDE: AG-DS-01, AG-DS-02, AG-DS-03
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDashboardMetricsResponse {

    /** 평균 마감 준수율 (%) */
    private Double averageDeadlineComplianceRate;

    /** 활동 작가 수 */
    private Long activeArtistCount;

    /** 진행 프로젝트 수 */
    private Long activeProjectCount;

    /** 평균 마감 준수율 전월 대비 변화 (선택) */
    private String complianceRateChange;

    /** 활동 작가 전월 대비 변화 (선택) */
    private String activeArtistChange;

    /** 진행 프로젝트 전월 대비 변화 (선택) */
    private String activeProjectChange;
}
