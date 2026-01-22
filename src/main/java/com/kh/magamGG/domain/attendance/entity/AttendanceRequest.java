package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ATTENDANCE_REQUEST")
@Getter
@NoArgsConstructor
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
	private LocalDate attendanceRequestStartDate;
	
	@Column(name = "ATTENDANCE_REQUEST_END_DATE", nullable = false)
	private LocalDate attendanceRequestEndDate;
	
	@Column(name = "ATTENDANCE_REQUEST_USING_DAYS", nullable = false)
	private Integer attendanceRequestUsingDays;
	
	@Column(name = "ATTENDANCE_REQUEST_REASON", length = 255)
	private String attendanceRequestReason;
	
	@Column(name = "WORKCATION_LOCATION", length = 100)
	private String workcationLocation;
	
	@Column(name = "MEDICAL_FILE_URL", nullable = false, length = 500)
	private String medicalFileUrl;
	
	@Column(name = "ATTENDANCE_REQUEST_STATUS", nullable = false, length = 20)
	private String attendanceRequestStatus;
	
	@Column(name = "ATTENDANCE_REQUEST_REJECT_REASON", length = 200)
	private String attendanceRequestRejectReason;
	
	@Column(name = "ATTENDANCE_REQUEST_CREATED_AT", nullable = false)
	private LocalDateTime attendanceRequestCreatedAt;
	
	@Column(name = "ATTENDANCE_REQUEST_UPADATED_AT")
	private LocalDateTime attendanceRequestUpadatedAt;
}
