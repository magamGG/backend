package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.request.LeaveBalanceAdjustRequest;
import com.kh.magamGG.domain.attendance.dto.response.AgencyMemberLeaveResponse;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveBalanceResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveHistoryResponse;

import java.util.List;

/**
 * 근태 관리 서비스 인터페이스
 */
public interface AttendanceService {
    
    /**
     * 근태 신청 생성
     * @param request 근태 신청 정보
     * @param memberNo 신청자 회원 번호
     * @return 생성된 근태 신청 정보
     */
    AttendanceRequestResponse createAttendanceRequest(AttendanceRequestCreateRequest request, Long memberNo);
    
    /**
     * 특정 회원의 근태 신청 목록 조회
     * @param memberNo 회원 번호
     * @return 근태 신청 목록
     */
    List<AttendanceRequestResponse> getAttendanceRequestsByMember(Long memberNo);
    
    /**
     * 대기 중인 근태 신청 목록 조회 (관리자용)
     * @return 대기 중인 근태 신청 목록
     */
    List<AttendanceRequestResponse> getPendingAttendanceRequests();
    
    /**
     * 특정 에이전시 소속 회원들의 근태 신청 목록 조회
     * @param agencyNo 에이전시 번호
     * @return 근태 신청 목록
     */
    List<AttendanceRequestResponse> getAttendanceRequestsByAgency(Long agencyNo);
    
    /**
     * 특정 에이전시 소속 회원들의 대기 중인 근태 신청 목록 조회
     * @param agencyNo 에이전시 번호
     * @return 대기 중인 근태 신청 목록
     */
    List<AttendanceRequestResponse> getPendingAttendanceRequestsByAgency(Long agencyNo);
    
    /**
     * 회원별 월별 근태 통계 조회
     * @param memberNo 회원 번호
     * @param year 연도
     * @param month 월
     * @return 근태 통계
     */
    AttendanceStatisticsResponseDto getAttendanceStatistics(Long memberNo, int year, int month);
    
    /**
     * 특정 회원의 현재 적용 중인 근태 상태 조회
     * - 승인(APPROVED)된 근태 신청 중
     * - 현재 날짜가 시작일~종료일 사이인 것
     * @param memberNo 회원 번호
     * @return 현재 적용 중인 근태 신청 정보 (없으면 null)
     */
    AttendanceRequestResponse getCurrentAttendanceStatus(Long memberNo);
    
    /**
     * 근태 신청 승인
     * @param attendanceRequestNo 근태 신청 번호
     * @return 승인된 근태 신청 정보
     */
    AttendanceRequestResponse approveAttendanceRequest(Long attendanceRequestNo);
    
    /**
     * 근태 신청 반려
     * @param attendanceRequestNo 근태 신청 번호
     * @param rejectReason 반려 사유
     * @return 반려된 근태 신청 정보
     */
    AttendanceRequestResponse rejectAttendanceRequest(Long attendanceRequestNo, String rejectReason);

    /**
     * 근태 신청 취소 (신청자 본인만, PENDING/REJECTED 상태에서만)
     * @param attendanceRequestNo 근태 신청 번호
     * @param memberNo 회원 번호 (본인 확인)
     * @return 취소된 근태 신청 정보
     */
    AttendanceRequestResponse cancelAttendanceRequest(Long attendanceRequestNo, Long memberNo);

    /**
     * 근태 신청 수정 (신청자 본인만, PENDING 상태에서만)
     * @param attendanceRequestNo 근태 신청 번호
     * @param request 수정할 근태 신청 정보
     * @param memberNo 회원 번호 (본인 확인)
     * @return 수정된 근태 신청 정보
     */
    AttendanceRequestResponse updateAttendanceRequest(Long attendanceRequestNo, AttendanceRequestCreateRequest request, Long memberNo);

    /**
     * 에이전시 소속 회원들의 연차 변경 이력(leaveHistory) 조회
     * @param agencyNo 에이전시 번호
     * @return 연차 변경 이력 목록
     */
    List<LeaveHistoryResponse> getLeaveHistoryByAgency(Long agencyNo);

    /**
     * 에이전시 소속 회원별 연차 잔액 목록 조회 (연차 관리 페이지 직원 리스트용)
     * @param agencyNo 에이전시 번호
     * @return 회원별 연차 정보 목록 (연차 없으면 0으로 반환)
     */
    List<AgencyMemberLeaveResponse> getLeaveBalancesByAgency(Long agencyNo);

    /**
     * 회원의 연차 잔액 조회 (당해 연도 기준, 없으면 최신 연도)
     * @param memberNo 회원 번호
     * @return 연차 잔액 (없으면 null)
     */
    LeaveBalanceResponse getLeaveBalance(Long memberNo);

    /**
     * 회원 연차 조정 (LeaveBalance 갱신 + LeaveHistory 생성)
     * @param memberNo 회원 번호
     * @param request 조정 일수, 사유, 메모
     * @return 갱신된 연차 잔액
     */
    LeaveBalanceResponse adjustLeaveBalance(Long memberNo, LeaveBalanceAdjustRequest request);

    /**
     * 출근 시작 (건강 체크 + 출근 기록)
     * @param healthCheckRequest 건강 체크 정보
     * @param memberNo 회원 번호
     * @return 출근 기록이 생성되었는지 여부
     */
    boolean startAttendance(com.kh.magamGG.domain.health.dto.request.DailyHealthCheckRequest healthCheckRequest, Long memberNo);
    
    /**
     * 오늘 날짜의 마지막 출근 상태 조회
     * @param memberNo 회원 번호
     * @return 마지막 출근 타입 ('출근' 또는 null)
     */
    String getTodayLastAttendanceType(Long memberNo);

    /**
     * 출근 종료 (퇴근 기록)
     * @param memberNo 회원 번호
     * @return 퇴근 기록 생성 여부
     */
    boolean endAttendance(Long memberNo);

    /**
     * 담당자의 담당 작가들 금주 근태 예정 (이번 주 월~일과 겹치는 근태 신청)
     * @param memberNo 로그인한 담당자 회원 번호
     * @return 담당 작가별 근태 신청 목록 (PENDING/APPROVED/REJECTED, CANCELLED 제외)
     */
    List<AttendanceRequestResponse> getWeeklyAttendanceByManager(Long memberNo);

    /**
     * 담당자의 담당 작가들 근태 신청 현황 전체 목록
     * - MANAGER → ARTIST_ASSIGNMENT → 담당 작가 MEMBER_NO 기준
     * - ATTENDANCE_REQUEST JOIN 후 신청일 최신순 정렬
     * - CANCELLED 상태는 제외
     *
     * @param memberNo 로그인한 담당자 회원 번호
     * @return 담당 작가들의 근태 신청 목록
     */
    List<AttendanceRequestResponse> getAttendanceRequestsByManager(Long memberNo);
}


