package com.kh.magamGG.domain.notification.controller;

import com.kh.magamGG.domain.notification.dto.response.NotificationResponse;
import com.kh.magamGG.domain.notification.entity.Notification;
import com.kh.magamGG.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 내 알림 목록 조회
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestHeader("X-Member-No") Long memberNo) {
        
        List<Notification> notifications = notificationService.getNotificationsByMember(memberNo);
        
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 알림 읽음 처리
     * PUT /api/notifications/{notificationNo}/read
     */
    @PutMapping("/{notificationNo}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        
        Notification notification = notificationService.markAsRead(notificationNo, memberNo);
        
        return ResponseEntity.ok(NotificationResponse.fromEntity(notification));
    }
    
    /**
     * 모든 알림 읽음 처리
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-Member-No") Long memberNo) {
        
        notificationService.markAllAsRead(memberNo);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 알림 삭제
     * DELETE /api/notifications/{notificationNo}
     */
    @DeleteMapping("/{notificationNo}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        
        notificationService.deleteNotification(notificationNo, memberNo);
        
        return ResponseEntity.ok().build();
    }
}
