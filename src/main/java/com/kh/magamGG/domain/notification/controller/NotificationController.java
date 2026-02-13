package com.kh.magamGG.domain.notification.controller;

import com.kh.magamGG.domain.notification.dto.response.NotificationResponse;
import com.kh.magamGG.domain.notification.entity.Notification;
import com.kh.magamGG.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    
    /**
     * SSE 구독 (실시간 알림 연결)
     * 
     * 인증 방식:
     * 1. JwtAuthenticationFilter에서 쿼리 파라미터 token을 읽어 인증 처리
     * 2. SecurityContext에 Authentication이 설정되어 있으면 사용
     * 3. 없으면 403 Forbidden (SecurityConfig의 .authenticated()에 의해)
     * 
     * EventSource는 헤더를 보낼 수 없으므로 token 쿼리 파라미터로 인증
     * GET /api/notifications/subscribe?token=xxx
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(value = "token", required = false) String token,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId,
            Authentication authentication) {

        // SecurityContext에서 인증된 사용자 정보 가져오기
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("SSE 구독 실패: 인증 정보 없음");
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        // Principal에서 회원번호 추출 (JwtAuthenticationFilter에서 설정한 값)
        Long memberNo;
        try {
            memberNo = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            log.error("SSE 구독 실패: 회원번호 파싱 오류 - {}", authentication.getPrincipal());
            throw new AccessDeniedException("유효하지 않은 인증 정보입니다.");
        }

        log.info("SSE 구독 시작: memberNo={}, lastEventId={}", memberNo, lastEventId);
        return notificationService.subscribe(memberNo, lastEventId);
    }

    /**
     * 내 알림 목록 조회
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            Authentication authentication) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        Long memberNo = Long.parseLong(authentication.getPrincipal().toString());
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
            Authentication authentication) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        Long memberNo = Long.parseLong(authentication.getPrincipal().toString());
        Notification notification = notificationService.markAsRead(notificationNo, memberNo);
        
        return ResponseEntity.ok(NotificationResponse.fromEntity(notification));
    }
    
    /**
     * 모든 알림 읽음 처리
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        Long memberNo = Long.parseLong(authentication.getPrincipal().toString());
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
            Authentication authentication) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        Long memberNo = Long.parseLong(authentication.getPrincipal().toString());
        notificationService.deleteNotification(notificationNo, memberNo);
        
        return ResponseEntity.ok().build();
    }
}
