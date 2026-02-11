package com.kh.magamGG.domain.notification.controller;

import com.kh.magamGG.domain.notification.dto.response.NotificationResponse;
import com.kh.magamGG.domain.notification.entity.Notification;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * SSE 구독 (실시간 알림 연결)
     * EventSource는 헤더를 보낼 수 없으므로 token 쿼리 파라미터로 인증
     * GET /api/notifications/subscribe?token=xxx
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(value = "token", required = true) String token,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        Long memberNo = jwtTokenProvider.getMemberNoFromToken(token);
        if (memberNo == null) {
            throw new IllegalArgumentException("토큰에서 회원 정보를 추출할 수 없습니다.");
        }

        return notificationService.subscribe(memberNo, lastEventId);
    }

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
                .collect(Collectors.toList());
        
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
