package com.kh.magamGG.domain.notification.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.entity.Notification;
import com.kh.magamGG.domain.notification.repository.NotificationRepository;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    
    @Override
    @Transactional
    public Notification createNotification(Long memberNo, String name, String text, String type) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        
        Notification notification = Notification.builder()
                .member(member)
                .notificationName(name)
                .notificationText(text)
                .notificationType(type)
                .notificationStatus("Y") // Y: 읽지 않음, N: 읽음
                .notificationCreatedAt(LocalDateTime.now())
                .build();
        
        Notification saved = notificationRepository.save(notification);
        log.info("알림 생성: 회원={}, 제목={}, 타입={}", member.getMemberName(), name, type);
        
        return saved;
    }
    
    @Override
    @Transactional
    public void notifyAgencyManagers(Long agencyNo, String name, String text, String type) {
        // 해당 에이전시 소속 담당자(매니저/관리자) 목록 조회
        // 알림을 받아야 하는 역할들
        List<Member> managers = memberRepository.findByAgency_AgencyNoAndMemberRoleIn(
                agencyNo, 
                List.of("담당자", "MANAGER", "매니저", "PD", "에이전시 관리자", "관리자", "대표")
        );
        
        if (managers.isEmpty()) {
            log.warn("에이전시 {}에 담당자가 없습니다. 알림을 보낼 수 없습니다.", agencyNo);
            return;
        }
        
        // 각 담당자에게 알림 생성
        for (Member manager : managers) {
            Notification notification = Notification.builder()
                    .member(manager)
                    .notificationName(name)
                    .notificationText(text)
                    .notificationType(type)
                    .notificationStatus("Y")
                    .notificationCreatedAt(LocalDateTime.now())
                    .build();
            
            notificationRepository.save(notification);
            log.info("담당자 알림 생성: 담당자={}, 제목={}", manager.getMemberName(), name);
        }
        
        log.info("에이전시 {} 담당자 {}명에게 알림 전송 완료", agencyNo, managers.size());
    }
    
    @Override
    public List<Notification> getNotificationsByMember(Long memberNo) {
        // JOIN FETCH로 Member 정보를 함께 조회하여 N+1 방지
        return notificationRepository.findByMemberNoWithMember(memberNo);
    }
    
    @Override
    @Transactional
    public Notification markAsRead(Long notificationNo, Long memberNo) {
        Notification notification = notificationRepository.findById(notificationNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        
        // 본인의 알림인지 확인
        if (!notification.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        
        notification.markAsRead();
        log.info("알림 읽음 처리: 알림번호={}, 회원번호={}", notificationNo, memberNo);
        
        return notification;
    }
    
    @Override
    @Transactional
    public void markAllAsRead(Long memberNo) {
        List<Notification> unreadNotifications = notificationRepository
                .findByMember_MemberNoAndNotificationStatusOrderByNotificationCreatedAtDesc(memberNo, "Y");
        
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        
        log.info("회원 {}의 모든 알림({})개 읽음 처리 완료", memberNo, unreadNotifications.size());
    }
    
    @Override
    @Transactional
    public void deleteNotification(Long notificationNo, Long memberNo) {
        Notification notification = notificationRepository.findById(notificationNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        
        // 본인의 알림인지 확인
        if (!notification.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 알림만 삭제할 수 있습니다.");
        }
        
        notificationRepository.delete(notification);
        log.info("알림 삭제: 알림번호={}, 회원번호={}", notificationNo, memberNo);
    }
}


