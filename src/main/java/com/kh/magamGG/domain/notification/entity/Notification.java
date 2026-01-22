package com.kh.magamGG.domain.notification.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
public class Notification {
	
	@Id
	@Column(name = "NOTIFICATION_NO")
	private Long notificationNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "NOTIFICATION_NAME", length = 30)
	private String notificationName;
	
	@Column(name = "NOTIFICATION_TEXT", length = 260)
	private String notificationText;
	
	@Column(name = "NOTIFICATION_CREATED_AT")
	private LocalDateTime notificationCreatedAt;
	
	@Column(name = "NOTIFICATION_STATUS", nullable = false, length = 1)
	private String notificationStatus;
	
	@Column(name = "NOTIFICATION_TYPE", length = 10)
	private String notificationType;
}
