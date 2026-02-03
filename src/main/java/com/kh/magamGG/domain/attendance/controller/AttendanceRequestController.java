package com.kh.magamGG.domain.attendance.controller;

import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.request.LeaveBalanceAdjustRequest;
import com.kh.magamGG.domain.attendance.dto.response.AgencyMemberLeaveResponse;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveBalanceResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveHistoryResponse;
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

    /**
     * 에이전시 소속 회원들의 연차 변경 이력(leaveHistory) 조회
     * GET /api/leave/agency/{agencyNo}/history
     *
     * @param agencyNo 에이전시 번호
     * @return 연차 변경 이력 목록
     */
    @GetMapping("/agency/{agencyNo}/history")
    public ResponseEntity<List<LeaveHistoryResponse>> getLeaveHistoryByAgency(@PathVariable Long agencyNo) {
        log.info("에이전시 {} 연차 변경 이력 조회", agencyNo);
        List<LeaveHistoryResponse> responses = attendanceService.getLeaveHistoryByAgency(agencyNo);
        return ResponseEntity.ok(responses);
    }

    /**
     * 에이전시 소속 회원별 연차 잔액 목록 조회 (연차 관리 페이지 직원 리스트용)
     * GET /api/leave/agency/{agencyNo}/balances
     *
     * @param agencyNo 에이전시 번호
     * @return 회원별 연차 정보 목록
     */
    @GetMapping("/agency/{agencyNo}/balances")
    public ResponseEntity<List<AgencyMemberLeaveResponse>> getLeaveBalancesByAgency(@PathVariable Long agencyNo) {
        log.info("에이전시 {} 연차 리스트 조회", agencyNo);
        List<AgencyMemberLeaveResponse> responses = attendanceService.getLeaveBalancesByAgency(agencyNo);
        return ResponseEntity.ok(responses);
    }

    /**
     * 회원 연차 잔액 조회 (당해 연도 기준)
     * GET /api/leave/balance/{memberNo}
     *
     * @param memberNo 회원 번호
     * @return 연차 잔액 (없으면 404 없음, null 반환 시 204 또는 200 with null body 정책에 따라 200 + body null)
     */
    @GetMapping("/balance/{memberNo}")
    public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(@PathVariable Long memberNo) {
        log.info("회원 {} 연차 잔액 조회", memberNo);
        LeaveBalanceResponse response = attendanceService.getLeaveBalance(memberNo);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 연차 조정 (LeaveBalance 갱신 + LeaveHistory 생성)
     * POST /api/leave/adjust-balance (body에 memberNo, adjustment, reason, note)
     *
     * @param request 대상 회원 번호, 조정 일수, 사유, 메모
     * @return 갱신된 연차 잔액
     */
    @PostMapping("/adjust-balance")
    public ResponseEntity<LeaveBalanceResponse> adjustLeaveBalance(@RequestBody LeaveBalanceAdjustRequest request) {
        Long memberNo = request.getMemberNo();
        if (memberNo == null) {
            throw new IllegalArgumentException("memberNo는 필수입니다.");
        }
        log.info("회원 {} 연차 조정 요청: 사유={}, 조정일수={}", memberNo, request.getReason(), request.getAdjustment());
        LeaveBalanceResponse response = attendanceService.adjustLeaveBalance(memberNo, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 회원의 현재 적용 중인 근태 상태 조회
     * GET /api/leave/current-status
     * 
     * - 승인(APPROVED)된 근태 신청 중
     * - 현재 날짜가 시작일~종료일 사이인 것
     * 
     * @param memberNo 회원 번호 (헤더에서 추출)
     * @return 현재 적용 중인 근태 상태 (없으면 null 반환)
     */
    @GetMapping("/current-status")
    public ResponseEntity<AttendanceRequestResponse> getCurrentAttendanceStatus(
            @RequestHeader("X-Member-No") Long memberNo) {
        
        log.info("회원 {}의 현재 근태 상태 조회", memberNo);
        
        AttendanceRequestResponse response = attendanceService.getCurrentAttendanceStatus(memberNo);
        
        if (response == null) {
            log.info("회원 {}의 현재 근태 상태 없음 - null 반환", memberNo);
            return ResponseEntity.ok(null);
        }
        
        log.info("회원 {}의 현재 근태 상태 반환: {}", memberNo, response.getAttendanceRequestType());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 근태 신청 승인
     * POST /api/leave/{attendanceRequestNo}/approve
     * 
     * @param attendanceRequestNo 근태 신청 번호
     * @return 승인된 근태 신청 정보
     */
    @PostMapping("/{attendanceRequestNo}/approve")
    public ResponseEntity<AttendanceRequestResponse> approveAttendanceRequest(
            @PathVariable Long attendanceRequestNo) {
        
        log.info("근태 신청 승인 요청: 신청번호={}", attendanceRequestNo);
        
        AttendanceRequestResponse response = attendanceService.approveAttendanceRequest(attendanceRequestNo);
        
        log.info("근태 신청 승인 완료: 신청번호={}", attendanceRequestNo);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 근태 신청 반려
     * POST /api/leave/{attendanceRequestNo}/reject
     * 
     * @param attendanceRequestNo 근태 신청 번호
     * @param rejectReason 반려 사유 (요청 본문)
     * @return 반려된 근태 신청 정보
     */
    @PostMapping("/{attendanceRequestNo}/reject")
    public ResponseEntity<AttendanceRequestResponse> rejectAttendanceRequest(
            @PathVariable Long attendanceRequestNo,
            @RequestBody(required = false) java.util.Map<String, String> requestBody) {
        
        String rejectReason = requestBody != null && requestBody.containsKey("rejectReason") 
                ? requestBody.get("rejectReason") 
                : "";
        
        log.info("근태 신청 반려 요청: 신청번호={}, 사유={}", attendanceRequestNo, rejectReason);
        
        AttendanceRequestResponse response = attendanceService.rejectAttendanceRequest(attendanceRequestNo, rejectReason);
        
        log.info("근태 신청 반려 완료: 신청번호={}", attendanceRequestNo);
        
        return ResponseEntity.ok(response);
    }
}
