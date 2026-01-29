package com.kh.magamGG.domain.notification.service;

import com.kh.magamGG.domain.notification.entity.Notification;

import java.util.List;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {
    
    /**
     * 알림 생성
     * @param memberNo 알림 받을 회원 번호
     * @param name 알림 제목
     * @param text 알림 내용
     * @param type 알림 타입 (JOIN_REQ, LEAVE_REQ 등)
     * @return 생성된 알림
     */
    Notification createNotification(Long memberNo, String name, String text, String type);
    
    /**
     * 에이전시 담당자들에게 알림 생성
     * @param agencyNo 에이전시 번호
     * @param name 알림 제목
     * @param text 알림 내용
     * @param type 알림 타입
     */
    void notifyAgencyManagers(Long agencyNo, String name, String text, String type);
    
    /**
     * 특정 회원의 알림 목록 조회
     * @param memberNo 회원 번호
     * @return 알림 목록
     */
    List<Notification> getNotificationsByMember(Long memberNo);
    
    /**
     * 알림 읽음 처리
     * @param notificationNo 알림 번호
     * @param memberNo 회원 번호 (권한 확인용)
     * @return 업데이트된 알림
     */
    Notification markAsRead(Long notificationNo, Long memberNo);
    
    /**
     * 모든 알림 읽음 처리
     * @param memberNo 회원 번호
     */
    void markAllAsRead(Long memberNo);
    
    /**
     * 알림 삭제
     * @param notificationNo 알림 번호
     * @param memberNo 회원 번호 (권한 확인용)
     */
    void deleteNotification(Long notificationNo, Long memberNo);
}


