package com.kh.magamGG.domain.attendance.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LEAVE_BALANCE")
public class LeaveBalance {
	
	@Id
	@Column(name = "LEAVE_BALANCE_NO")
	private Long leaveBalanceNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "LEAVE_TYPE", nullable = false, length = 20)
	private String leaveType;
	
	@Column(name = "LEAVE_BALANCE_TOTAL_DAYS", nullable = false)
	private Long leaveBalanceTotalDays;
	
	@Column(name = "LEAVE_BALANCE_USED_DAYS")
	private Long leaveBalanceUsedDays;
	
	@Column(name = "LEAVE_BALANCE_REMAIN_DAYS")
	private Long leaveBalanceRemainDays;
	
	@Column(name = "LEAVE_BALANCE_YEAR", nullable = false, length = 4)
	private String leaveBalanceYear;
	
	@Column(name = "LEAVE_BALANCE_UPDATED_AT")
	private LocalDateTime leaveBalanceUpdatedAt;
}
