package com.kh.magamGG.domain.attendance.dto.response;

import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 근태 신청 응답 DTO
 * DB 필드: ATTENDANCE_REQUEST 테이블
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequestResponse {
    
    /**
     * 근태 신청 번호
     * DB: ATTENDANCE_REQUEST_NO
     */
    private Long attendanceRequestNo;
    
    /**
     * 회원 번호
     * DB: MEMBER_NO
     */
    private Long memberNo;
    
    /**
     * 회원 이름 (JOIN 결과)
     */
    private String memberName;
    
    /**
     * 근태 신청 타입
     * DB: ATTENDANCE_REQUEST_TYPE
     */
    private String attendanceRequestType;
    
    /**
     * 시작 날짜
     * DB: ATTENDANCE_REQUEST_START_DATE
     */
    private LocalDateTime attendanceRequestStartDate;
    
    /**
     * 종료 날짜
     * DB: ATTENDANCE_REQUEST_END_DATE
     */
    private LocalDateTime attendanceRequestEndDate;
    
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
     * 워케이션 장소
     * DB: WORKCATION_LOCATION
     */
    private String workcationLocation;
    
    /**
     * 의료 파일 URL
     * DB: MEDICAL_FILE_URL
     */
    private String medicalFileUrl;
    
    /**
     * 신청 상태 (PENDING | APPROVED | REJECTED | CANCELLED)
     * DB: ATTENDANCE_REQUEST_STATUS
     */
    private String attendanceRequestStatus;
    
    /**
     * 반려 사유
     * DB: ATTENDANCE_REQUEST_REJECT_REASON
     */
    private String attendanceRequestRejectReason;
    
    /**
     * 생성 일시
     * DB: ATTENDANCE_REQUEST_CREATED_AT
     */
    private LocalDateTime attendanceRequestCreatedAt;
    
    /**
     * 수정 일시
     * DB: ATTENDANCE_REQUEST_UPDATED_AT
     */
    private LocalDateTime attendanceRequestUpdatedAt;
    
    /**
     * Entity -> Response DTO 변환
     */
    public static AttendanceRequestResponse fromEntity(AttendanceRequest entity) {
        return AttendanceRequestResponse.builder()
                .attendanceRequestNo(entity.getAttendanceRequestNo())
                .memberNo(entity.getMember().getMemberNo())
                .memberName(entity.getMember().getMemberName())
                .attendanceRequestType(entity.getAttendanceRequestType())
                .attendanceRequestStartDate(entity.getAttendanceRequestStartDate())
                .attendanceRequestEndDate(entity.getAttendanceRequestEndDate())
                .attendanceRequestUsingDays(entity.getAttendanceRequestUsingDays())
                .attendanceRequestReason(entity.getAttendanceRequestReason())
                .workcationLocation(entity.getWorkcationLocation())
                .medicalFileUrl(entity.getMedicalFileUrl())
                .attendanceRequestStatus(entity.getAttendanceRequestStatus())
                .attendanceRequestRejectReason(entity.getAttendanceRequestRejectReason())
                .attendanceRequestCreatedAt(entity.getAttendanceRequestCreatedAt())
                .attendanceRequestUpdatedAt(entity.getAttendanceRequestUpdatedAt())
                .build();
    }
}
