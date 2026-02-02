package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ATTENDANCE_REQUEST_NO")
	private Long attendanceRequestNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "ATTENDANCE_REQUEST_TYPE", nullable = false, length = 20)
	private String attendanceRequestType;
	
	@Column(name = "ATTENDANCE_REQUEST_START_DATE", nullable = false)
	private LocalDateTime attendanceRequestStartDate;
	
	@Column(name = "ATTENDANCE_REQUEST_END_DATE", nullable = false)
	private LocalDateTime attendanceRequestEndDate;
	
	@Column(name = "ATTENDANCE_REQUEST_USING_DAYS", nullable = false)
	private Integer attendanceRequestUsingDays;
	
	@Column(name = "ATTENDANCE_REQUEST_REASON", length = 255)
	private String attendanceRequestReason;
	
	@Column(name = "WORKCATION_LOCATION", length = 100)
	private String workcationLocation;
	
	@Column(name = "MEDICAL_FILE_URL", nullable = false, length = 500)
	private String medicalFileUrl;
	
	@Column(name = "ATTENDANCE_REQUEST_STATUS", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
	private String attendanceRequestStatus;
	
	@Column(name = "ATTENDANCE_REQUEST_REJECT_REASON", length = 200)
	private String attendanceRequestRejectReason;
	
	@Column(name = "ATTENDANCE_REQUEST_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime attendanceRequestCreatedAt;
	
	@Column(name = "ATTENDANCE_REQUEST_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime attendanceRequestUpdatedAt;
	
	/**
	 * 근태 신청 승인 처리
	 */
	public void approve() {
		this.attendanceRequestStatus = "APPROVED";
		this.attendanceRequestUpdatedAt = LocalDateTime.now();
	}
	
	/**
	 * 근태 신청 반려 처리
	 * @param rejectReason 반려 사유
	 */
	public void reject(String rejectReason) {
		this.attendanceRequestStatus = "REJECTED";
		this.attendanceRequestRejectReason = rejectReason;
		this.attendanceRequestUpdatedAt = LocalDateTime.now();
	}
}
