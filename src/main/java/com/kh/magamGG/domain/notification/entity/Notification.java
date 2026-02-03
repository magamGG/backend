package com.kh.magamGG.domain.notification.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "NOTIFICATION_NO")
	private Long notificationNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "NOTIFICATION_NAME", length = 30)
	private String notificationName;
	
	@Column(name = "NOTIFICATION_TEXT", length = 260)
	private String notificationText;
	
	@Column(name = "NOTIFICATION_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime notificationCreatedAt;
	
	@Column(name = "NOTIFICATION_STATUS", nullable = false, length = 1)
	private String notificationStatus;
	
	@Column(name = "NOTIFICATION_TYPE", length = 10)
	private String notificationType;
	
	/**
	 * 알림 읽음 처리
	 */
	public void markAsRead() {
		this.notificationStatus = "N";
	}
	
	/**
	 * 알림이 읽지 않은 상태인지 확인
	 */
	public boolean isUnread() {
		return "Y".equals(this.notificationStatus);
	}
}
