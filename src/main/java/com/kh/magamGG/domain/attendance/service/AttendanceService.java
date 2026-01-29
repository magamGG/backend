package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;

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
}


