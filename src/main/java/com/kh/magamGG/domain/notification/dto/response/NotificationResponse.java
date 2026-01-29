package com.kh.magamGG.domain.notification.dto.response;

import com.kh.magamGG.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private Long notificationNo;
    private Long memberNo;
    private String notificationName;
    private String notificationText;
    private String notificationType;
    private String notificationStatus; // Y: 읽지 않음, N: 읽음
    private LocalDateTime notificationCreatedAt;
    private boolean isRead;
    
    /**
     * Entity -> Response DTO 변환
     */
    public static NotificationResponse fromEntity(Notification entity) {
        return NotificationResponse.builder()
                .notificationNo(entity.getNotificationNo())
                .memberNo(entity.getMember().getMemberNo())
                .notificationName(entity.getNotificationName())
                .notificationText(entity.getNotificationText())
                .notificationType(entity.getNotificationType())
                .notificationStatus(entity.getNotificationStatus())
                .notificationCreatedAt(entity.getNotificationCreatedAt())
                .isRead(!entity.isUnread()) // Y면 읽지않음(false), N이면 읽음(true)
                .build();
    }
}
