package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ATTENDANCE")
@Getter
@Setter
@NoArgsConstructor
public class Attendance {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ATTENDANCE_NO")
	private Long attendanceNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AGENCY_NO", nullable = false)
	private Agency agency;
	
	@Column(name = "ATTENDANCE_TYPE", nullable = false, length = 10)
	private String attendanceType;
	
	@Column(name = "ATTENDANCE_TIME", nullable = false)
	private LocalDateTime attendanceTime;
}
