package com.kh.magamGG.domain.notification.repository;

import com.kh.magamGG.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 특정 회원의 알림 목록 조회 (최신순) - Member JOIN FETCH로 N+1 방지
     */
    @Query("SELECT n FROM Notification n " +
           "JOIN FETCH n.member m " +
           "WHERE m.memberNo = :memberNo " +
           "ORDER BY n.notificationCreatedAt DESC")
    List<Notification> findByMemberNoWithMember(@Param("memberNo") Long memberNo);
    
    /**
     * 특정 회원의 읽지 않은 알림 목록 조회
     */
    List<Notification> findByMember_MemberNoAndNotificationStatusOrderByNotificationCreatedAtDesc(Long memberNo, String status);
}


