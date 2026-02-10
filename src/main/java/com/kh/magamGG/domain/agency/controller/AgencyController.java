package com.kh.magamGG.domain.agency.controller;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.RejectJoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.UpdateAgencyLeaveRequest;
import com.kh.magamGG.domain.agency.dto.request.UpdateHealthScheduleRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDetailResponse;
import com.kh.magamGG.domain.agency.dto.response.ArtistDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.AttendanceDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDeadlineCountResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyHealthScheduleResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyUnscreenedListResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthMonitoringDetailResponse;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
public class AgencyController {
    
    private final AgencyService agencyService;
    
    @PostMapping("/join-request")
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @RequestBody JoinRequestRequest request,
            @RequestHeader("X-Member-No") Long memberNo) {
        JoinRequestResponse response = agencyService.createJoinRequest(request, memberNo);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 에이전시 대시보드 메트릭 (평균 마감 준수율, 활동 작가, 진행 프로젝트)
     * GET /api/agency/{agencyNo}/dashboard-metrics
     */
    @GetMapping("/{agencyNo}/dashboard-metrics")
    public ResponseEntity<AgencyDashboardMetricsResponse> getDashboardMetrics(@PathVariable Long agencyNo) {
        AgencyDashboardMetricsResponse response = agencyService.getDashboardMetrics(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 평균 마감 준수율 추이 (월별 + 전월 대비)
     * GET /api/agency/{agencyNo}/compliance-trend
     */
    @GetMapping("/{agencyNo}/compliance-trend")
    public ResponseEntity<ComplianceTrendResponse> getComplianceTrend(@PathVariable Long agencyNo) {
        ComplianceTrendResponse response = agencyService.getComplianceTrend(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 작품별 아티스트 분포도
     * GET /api/agency/{agencyNo}/artist-distribution
     */
    @GetMapping("/{agencyNo}/artist-distribution")
    public ResponseEntity<ArtistDistributionResponse> getArtistDistribution(@PathVariable Long agencyNo) {
        ArtistDistributionResponse response = agencyService.getArtistDistribution(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 금일 출석 현황
     * GET /api/agency/{agencyNo}/attendance-distribution
     */
    @GetMapping("/{agencyNo}/attendance-distribution")
    public ResponseEntity<AttendanceDistributionResponse> getAttendanceDistribution(@PathVariable Long agencyNo) {
        AttendanceDistributionResponse response = agencyService.getAttendanceDistribution(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 건강 인원 분포
     * GET /api/agency/{agencyNo}/health-distribution
     */
    @GetMapping("/{agencyNo}/health-distribution")
    public ResponseEntity<HealthDistributionResponse> getHealthDistribution(@PathVariable Long agencyNo) {
        HealthDistributionResponse response = agencyService.getHealthDistribution(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 검진 모니터링 상세 목록 (정신/신체 타입별)
     * GET /api/agency/{agencyNo}/health-monitoring-detail?type=mental|physical
     */
    @GetMapping("/{agencyNo}/health-monitoring-detail")
    public ResponseEntity<HealthMonitoringDetailResponse> getHealthMonitoringDetail(
            @PathVariable Long agencyNo,
            @RequestParam(defaultValue = "mental") String type) {
        HealthMonitoringDetailResponse response = agencyService.getHealthMonitoringDetail(agencyNo, type);
        return ResponseEntity.ok(response);
    }

    /**
     * 에이전시 건강 검진 일정 (HEALTH_SURVEY 생성일·주기 기반 다음 검진 예정일)
     * GET /api/agency/{agencyNo}/health-schedule
     */
    @GetMapping("/{agencyNo}/health-schedule")
    public ResponseEntity<AgencyHealthScheduleResponse> getAgencyHealthSchedule(@PathVariable Long agencyNo) {
        AgencyHealthScheduleResponse response = agencyService.getAgencyHealthSchedule(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 에이전시 건강 검진 설정 수정 (HEALTH_SURVEY period, cycle)
     * PUT /api/agency/{agencyNo}/health-schedule
     */
    @PutMapping("/{agencyNo}/health-schedule")
    public ResponseEntity<Void> updateAgencyHealthSchedule(
            @PathVariable Long agencyNo,
            @RequestBody UpdateHealthScheduleRequest request) {
        agencyService.updateAgencyHealthSchedule(agencyNo, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 에이전시 미검진 인원 목록 (정신/신체 중 하나라도 미검진이면 포함)
     * GET /api/agency/{agencyNo}/unscreened-list
     */
    @GetMapping("/{agencyNo}/unscreened-list")
    public ResponseEntity<AgencyUnscreenedListResponse> getAgencyUnscreenedList(@PathVariable Long agencyNo) {
        AgencyUnscreenedListResponse response = agencyService.getAgencyUnscreenedList(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 미검진 인원 1명에게 검진 알림 발송
     * POST /api/agency/{agencyNo}/unscreened-notify/{memberNo}
     */
    @PostMapping("/{agencyNo}/unscreened-notify/{memberNo}")
    public ResponseEntity<Void> sendUnscreenedNotification(
            @PathVariable Long agencyNo,
            @PathVariable Long memberNo) {
        agencyService.sendUnscreenedNotification(agencyNo, memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 7일 이상 지연된 미검진 인원에게 검진 알림 일괄 발송
     * POST /api/agency/{agencyNo}/unscreened-notify-bulk
     */
    @PostMapping("/{agencyNo}/unscreened-notify-bulk")
    public ResponseEntity<Void> sendUnscreenedBulkNotification(@PathVariable Long agencyNo) {
        agencyService.sendUnscreenedBulkNotification(agencyNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 에이전시 마감 임박 현황 (담당자 관리 프로젝트 업무 기준, 오늘~4일 후 5개 집계)
     * GET /api/agency/{agencyNo}/deadline-counts
     */
    @GetMapping("/{agencyNo}/deadline-counts")
    public ResponseEntity<List<AgencyDeadlineCountResponse.DeadlineItem>> getAgencyDeadlineCounts(@PathVariable Long agencyNo) {
        List<AgencyDeadlineCountResponse.DeadlineItem> counts = agencyService.getAgencyDeadlineCounts(agencyNo);
        return ResponseEntity.ok(counts);
    }

    /**
     * 에이전시 상세 조회 (agencyLeave 등)
     * GET /api/agency/{agencyNo}
     */
    @GetMapping("/{agencyNo}")
    public ResponseEntity<AgencyDetailResponse> getAgency(@PathVariable Long agencyNo) {
        Agency agency = agencyService.getAgency(agencyNo);
        AgencyDetailResponse response = AgencyDetailResponse.builder()
                .agencyNo(agency.getAgencyNo())
                .agencyName(agency.getAgencyName())
                .agencyCode(agency.getAgencyCode())
                .agencyLeave(agency.getAgencyLeave() != null ? agency.getAgencyLeave() : 15)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{agencyNo}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(@PathVariable Long agencyNo) {
        List<JoinRequestResponse> responses = agencyService.getJoinRequests(agencyNo);
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 회원의 대기 중인 가입 요청 조회
     * GET /api/agency/my-join-request
     */
    @GetMapping("/my-join-request")
    public ResponseEntity<JoinRequestResponse> getMyJoinRequest(
            @RequestHeader("X-Member-No") Long memberNo) {
        JoinRequestResponse response = agencyService.getMyPendingJoinRequest(memberNo);
        if (response == null) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join-requests/{newRequestNo}/approve")
    public ResponseEntity<JoinRequestResponse> approveJoinRequest(@PathVariable Long newRequestNo) {
        JoinRequestResponse response = agencyService.approveJoinRequest(newRequestNo);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join-requests/{newRequestNo}/reject")
    public ResponseEntity<JoinRequestResponse> rejectJoinRequest(
            @PathVariable Long newRequestNo,
            @RequestBody(required = false) RejectJoinRequestRequest request) {
        String rejectionReason = request != null ? request.getRejectionReason() : null;
        JoinRequestResponse response = agencyService.rejectJoinRequest(newRequestNo, rejectionReason);
        return ResponseEntity.ok(response);
    }

    /**
     * 에이전시 기본 연차(agencyLeave) 수정
     * PUT /api/agency/{agencyNo}/leave
     */
    @PutMapping("/{agencyNo}/leave")
    public ResponseEntity<Void> updateAgencyLeave(
            @PathVariable Long agencyNo,
            @RequestBody UpdateAgencyLeaveRequest request) {
        agencyService.updateAgencyLeave(agencyNo, request.getAgencyLeave());
        return ResponseEntity.noContent().build();
    }
}
