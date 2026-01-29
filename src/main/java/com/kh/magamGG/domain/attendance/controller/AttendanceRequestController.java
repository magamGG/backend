package com.kh.magamGG.domain.attendance.controller;

import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 근태 신청 컨트롤러
 * 근태 신청 관련 API 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
@Slf4j
public class AttendanceRequestController {
    
    private final AttendanceService attendanceService;
    
    /**
     * 근태 신청 생성
     * POST /api/leave/request
     * 
     * @param request 근태 신청 정보
     * @param memberNo 신청자 회원 번호 (헤더에서 추출)
     * @return 생성된 근태 신청 정보
     */
    @PostMapping("/request")
    public ResponseEntity<AttendanceRequestResponse> createAttendanceRequest(
            @RequestBody AttendanceRequestCreateRequest request,
            @RequestHeader("X-Member-No") Long memberNo) {
        
        log.info("근태 신청 요청: 회원번호={}, 타입={}", memberNo, request.getAttendanceRequestType());
        
        AttendanceRequestResponse response = attendanceService.createAttendanceRequest(request, memberNo);
        
        log.info("근태 신청 완료: 신청번호={}", response.getAttendanceRequestNo());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 내 근태 신청 목록 조회
     * GET /api/leave/my-requests
     * 
     * @param memberNo 회원 번호 (헤더에서 추출)
     * @return 근태 신청 목록
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<AttendanceRequestResponse>> getMyAttendanceRequests(
            @RequestHeader("X-Member-No") Long memberNo) {
        
        List<AttendanceRequestResponse> responses = attendanceService.getAttendanceRequestsByMember(memberNo);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 대기 중인 근태 신청 목록 조회 (관리자/담당자용)
     * GET /api/leave/pending
     * 
     * @return 대기 중인 근태 신청 목록
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AttendanceRequestResponse>> getPendingAttendanceRequests() {
        
        List<AttendanceRequestResponse> responses = attendanceService.getPendingAttendanceRequests();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 특정 에이전시 소속 회원들의 근태 신청 목록 조회
     * GET /api/leave/agency/{agencyNo}
     * 
     * @param agencyNo 에이전시 번호
     * @return 해당 에이전시 소속 회원들의 근태 신청 목록
     */
    @GetMapping("/agency/{agencyNo}")
    public ResponseEntity<List<AttendanceRequestResponse>> getAttendanceRequestsByAgency(
            @PathVariable Long agencyNo) {
        
        log.info("에이전시 {} 소속 근태 신청 목록 조회", agencyNo);
        
        List<AttendanceRequestResponse> responses = attendanceService.getAttendanceRequestsByAgency(agencyNo);
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 특정 에이전시 소속 회원들의 대기 중인 근태 신청 목록 조회
     * GET /api/leave/agency/{agencyNo}/pending
     * 
     * @param agencyNo 에이전시 번호
     * @return 해당 에이전시 소속 회원들의 대기 중인 근태 신청 목록
     */
    @GetMapping("/agency/{agencyNo}/pending")
    public ResponseEntity<List<AttendanceRequestResponse>> getPendingAttendanceRequestsByAgency(
            @PathVariable Long agencyNo) {
        
        log.info("에이전시 {} 소속 대기 중인 근태 신청 목록 조회", agencyNo);
        
        List<AttendanceRequestResponse> responses = attendanceService.getPendingAttendanceRequestsByAgency(agencyNo);
        
        return ResponseEntity.ok(responses);
    }
}
