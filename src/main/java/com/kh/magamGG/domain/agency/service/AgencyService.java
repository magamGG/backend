package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.ArtistDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.AttendanceDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;

import java.util.List;

public interface AgencyService {
    JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo);
    List<JoinRequestResponse> getJoinRequests(Long agencyNo);
    JoinRequestResponse approveJoinRequest(Long newRequestNo);
    JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason);

    /**
     * 에이전시 정보 조회
     */
    Agency getAgency(Long agencyNo);

    /**
     * 회사 코드로 에이전시 조회
     */
    Agency getAgencyByCode(String agencyCode);

    /**
     * 에이전시 소속명(스튜디오) 수정
     */
    void updateAgencyName(Long agencyNo, String agencyName);

    /**
     * 에이전시 기본 연차(agencyLeave) 수정
     */
    void updateAgencyLeave(Long agencyNo, Integer agencyLeave);

    /**
     * 에이전시 대시보드 메트릭 조회 (평균 마감 준수율, 활동 작가, 진행 프로젝트)
     */
    AgencyDashboardMetricsResponse getDashboardMetrics(Long agencyNo);

    /**
     * 평균 마감 준수율 추이 (월별 + 전월 대비)
     */
    ComplianceTrendResponse getComplianceTrend(Long agencyNo);

    /**
     * 작품별 아티스트 분포도
     */
    ArtistDistributionResponse getArtistDistribution(Long agencyNo);

    /**
     * 금일 출석 현황 (출근, 재택근무, 휴가, 워케이션, 미출석)
     */
    AttendanceDistributionResponse getAttendanceDistribution(Long agencyNo);

    /**
     * 건강 인원 분포 (위험, 주의, 정상)
     */
    HealthDistributionResponse getHealthDistribution(Long agencyNo);
}
