package com.kh.magamGG.domain.member.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.entity.LeaveHistory;
import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import com.kh.magamGG.domain.health.entity.HealthSurveyResponse;
import com.kh.magamGG.domain.memo.entity.Memo;
import com.kh.magamGG.domain.notification.entity.Notification;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.entity.TaskHistory;
import com.kh.magamGG.domain.member.entity.NewRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MEMBER")
@Getter
@NoArgsConstructor
public class Member {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEMBER_NO")
	private Long memberNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AGENCY_NO")
	private Agency agency;
	
	@Column(name = "MEMBER_NAME", nullable = false, length = 20)
	private String memberName;
	
	@Column(name = "MEMBER_PASSWORD", nullable = false, length = 100)
	private String memberPassword;
	
	@Column(name = "MEMBER_EMAIL", nullable = false, length = 50, unique = true)
	private String memberEmail;
	
	@Column(name = "MEMBER_PHONE", nullable = false, length = 15)
	private String memberPhone;

    @Column(name = "MEMBER_ADDRESS", length = 100)
    private String memberAddress;
	
	@Column(name = "MEMBER_STATUS", nullable = false, length = 20)
	private String memberStatus;
	
	@Column(name = "MEMBER_PROFILE_IMAGE", length = 100)
	private String memberProfileImage;
	
	@Column(name = "MEMBER_PROFILE_BANNER_IMAGE", length = 100)
	private String memberProfileBannerImage;
	
	@Column(name = "MEMBER_ROLE", nullable = false, length = 20)
	private String memberRole;
	
	@Column(name = "MEMBER_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime memberCreatedAt;
	
	@Column(name = "MEMBER_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime memberUpdatedAt;
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Attendance> attendances = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AttendanceRequest> attendanceRequests = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LeaveBalance> leaveBalances = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProjectMember> projectMembers = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CalendarEvent> calendarEvents = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Memo> memos = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Notification> notifications = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HealthSurveyResponse> healthSurveyResponses = new ArrayList<>();
	
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TaskHistory> taskHistories = new ArrayList<>();

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyHealthCheck> dailyHealthChecks = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewRequest> newRequests = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveHistory> leaveHistories = new ArrayList<>();
    
    // 프로필 정보 수정
    public void updateProfile(String memberName, String memberEmail, String memberPhone, String memberAddress) {
    	this.memberName = memberName;
    	this.memberEmail = memberEmail;
    	this.memberPhone = memberPhone;
    	this.memberAddress = memberAddress;
    	this.memberUpdatedAt = LocalDateTime.now();
    }
    
    // 프로필 이미지 업데이트
    public void updateProfileImage(String memberProfileImage) {
    	this.memberProfileImage = memberProfileImage;
    	this.memberUpdatedAt = LocalDateTime.now();
    }
    
    // 배경 이미지 업데이트
    public void updateBackgroundImage(String memberProfileBannerImage) {
    	this.memberProfileBannerImage = memberProfileBannerImage;
    	this.memberUpdatedAt = LocalDateTime.now();
    }
}
