package com.kh.magamGG.domain.manager.controller;

import com.kh.magamGG.domain.agency.dto.response.AgencyHealthScheduleResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyUnscreenedListResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthMonitoringDetailResponse;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.manager.dto.response.AssignedArtistResponse;
import com.kh.magamGG.domain.manager.service.ArtistAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ArtistAssignmentService assignmentService;
    private final AgencyService agencyService;

    /**
     * 담당자 배정 작가(ARTIST_ASSIGNMENT)만 대상 건강 인원 분포 (정신/신체 도넛 차트용)
     * GET /api/managers/health-distribution (X-Member-No: 담당자 memberNo)
     */
    @GetMapping("/health-distribution")
    public ResponseEntity<HealthDistributionResponse> getHealthDistribution(
            @RequestHeader("X-Member-No") Long memberNo) {
        return ResponseEntity.ok(agencyService.getHealthDistributionForManager(memberNo));
    }

    /**
     * 담당자 소속 에이전시 기준 건강 검진 일정
     * GET /api/managers/health-schedule (X-Member-No: 담당자 memberNo)
     */
    @GetMapping("/health-schedule")
    public ResponseEntity<AgencyHealthScheduleResponse> getHealthSchedule(
            @RequestHeader("X-Member-No") Long memberNo) {
        return ResponseEntity.ok(agencyService.getHealthScheduleForManager(memberNo));
    }

    /**
     * 담당자 배정 작가 중 미검진 인원 목록
     * GET /api/managers/unscreened-list (X-Member-No: 담당자 memberNo)
     */
    @GetMapping("/unscreened-list")
    public ResponseEntity<AgencyUnscreenedListResponse> getUnscreenedList(
            @RequestHeader("X-Member-No") Long memberNo) {
        return ResponseEntity.ok(agencyService.getUnscreenedListForManager(memberNo));
    }

    /**
     * 담당자 배정 작가 검진 모니터링 상세 (정신/신체 타입별)
     * GET /api/managers/health-monitoring-detail?type=mental|physical
     */
    @GetMapping("/health-monitoring-detail")
    public ResponseEntity<HealthMonitoringDetailResponse> getHealthMonitoringDetail(
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestParam(defaultValue = "mental") String type) {
        return ResponseEntity.ok(agencyService.getHealthMonitoringDetailForManager(memberNo, type));
    }

    @PostMapping("/{managerNo}/artists/{artistNo}")
    public void assignArtist(
            @PathVariable Long managerNo,
            @PathVariable Long artistNo
    ) {
        assignmentService.assignArtist(managerNo, artistNo);
    }

    @GetMapping("/{managerNo}/artists")
    public List<AssignedArtistResponse> getAssignedArtists(
            @PathVariable Long managerNo
    ) {
        return assignmentService.getAssignedArtists(managerNo)
                .stream()
                .map(AssignedArtistResponse::from)
                .toList();
    }

    @GetMapping("/{managerNo}/working-artists")
    public List<AssignedArtistResponse> getWorkingArtists(
            @PathVariable Long managerNo
    ) {
        return assignmentService.getWorkingArtistResponses(managerNo);
    }
}