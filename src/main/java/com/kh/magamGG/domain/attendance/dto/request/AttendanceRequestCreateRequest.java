package com.kh.magamGG.domain.attendance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 근태 신청 요청 DTO
 * DB 필드: ATTENDANCE_REQUEST 테이블
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequestCreateRequest {
    
    /**
     * 근태 신청 타입
     * 연차 | 반차 | 반반차 | 병가 | 워케이션 | 재택근무 | 휴재
     * DB: ATTENDANCE_REQUEST_TYPE
     */
    private String attendanceRequestType;
    
    /**
     * 시작 날짜 (ISO 8601 형식: YYYY-MM-DDTHH:mm:ss 또는 YYYY-MM-DD)
     * DB: ATTENDANCE_REQUEST_START_DATE
     */
    private String attendanceRequestStartDate;
    
    /**
     * 종료 날짜 (ISO 8601 형식: YYYY-MM-DDTHH:mm:ss 또는 YYYY-MM-DD)
     * DB: ATTENDANCE_REQUEST_END_DATE
     */
    private String attendanceRequestEndDate;
    
    /**
     * 사용 일수
     * DB: ATTENDANCE_REQUEST_USING_DAYS
     */
    private Integer attendanceRequestUsingDays;
    
    /**
     * 신청 사유
     * DB: ATTENDANCE_REQUEST_REASON
     */
    private String attendanceRequestReason;
    
    /**
     * 워케이션 장소 (워케이션 타입인 경우)
     * DB: WORKCATION_LOCATION
     */
    private String workcationLocation;
    
    /**
     * 의료 파일 URL (병가인 경우)
     * DB: MEDICAL_FILE_URL
     */
    private String medicalFileUrl;
}
