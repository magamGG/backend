package com.kh.magamGG.domain.agency.controller;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.RejectJoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.UpdateAgencyLeaveRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDetailResponse;
import com.kh.magamGG.domain.agency.dto.response.*;
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

    @GetMapping("/{agencyNo}/dashboard-metrics")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(@PathVariable Long agencyNo) {
        return ResponseEntity.ok(agencyService.getDashboardMetrics(agencyNo));
    }

    @GetMapping("/{agencyNo}/compliance-trend")
    public ResponseEntity<ComplianceTrendResponse> getComplianceTrend(@PathVariable Long agencyNo) {
        return ResponseEntity.ok(agencyService.getComplianceTrend(agencyNo));
    }

    @GetMapping("/{agencyNo}/artist-distribution")
    public ResponseEntity<ArtistDistributionResponse> getArtistDistribution(@PathVariable Long agencyNo) {
        return ResponseEntity.ok(agencyService.getArtistDistribution(agencyNo));
    }

    @GetMapping("/{agencyNo}/attendance-distribution")
    public ResponseEntity<AttendanceDistributionResponse> getAttendanceDistribution(@PathVariable Long agencyNo) {
        return ResponseEntity.ok(agencyService.getAttendanceDistribution(agencyNo));
    }

    @GetMapping("/{agencyNo}/health-distribution")
    public ResponseEntity<HealthDistributionResponse> getHealthDistribution(@PathVariable Long agencyNo) {
        return ResponseEntity.ok(agencyService.getHealthDistribution(agencyNo));
    }
}
