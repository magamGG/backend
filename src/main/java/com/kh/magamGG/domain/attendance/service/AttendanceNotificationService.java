package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 근태 신청 알림 발송 비즈니스 로직 분리 (비동기 처리)
 * 가이드 준수: attendance 도메인 내 새 클래스로 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceNotificationService {
    
    private final NotificationService notificationService;
    
    /**
     * 근태 신청 생성 시 알림 발송 (비동기)
     * @param member 신청자 회원 정보
     * @param requestType 근태 신청 타입
     * @param startDate 시작일 (LocalDateTime)
     * @param endDate 종료일 (LocalDateTime)
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> sendAttendanceRequestNotification(Member member, String requestType, 
                                                                      java.time.LocalDateTime startDate, 
                                                                      java.time.LocalDateTime endDate) {
        log.info("🔔 [알림] 근태 신청 알림 발송 시작: memberNo={}, memberName={}, requestType={}, agencyNo={}", 
            member.getMemberNo(), 
            member.getMemberName(),
            requestType,
            member.getAgency() != null ? member.getAgency().getAgencyNo() : "null");
        
        try {
            // 근태 신청(LEAVE_REQ) 알림은 에이전시 관리자에게만 발송
            if (member.getAgency() != null) {
                String notificationName = "근태 신청";
                String notificationText = String.format("%s님이 %s을(를) 신청했습니다. (%s ~ %s)",
                        member.getMemberName(),
                        requestType,
                        startDate.toLocalDate(),
                        endDate.toLocalDate());

                log.info("🔔 [알림] notifyAgencyManagers 호출: agencyNo={}, notificationName={}, notificationText={}", 
                    member.getAgency().getAgencyNo(), notificationName, notificationText);
                
                notificationService.notifyAgencyManagers(
                        member.getAgency().getAgencyNo(),
                        notificationName,
                        notificationText,
                        "LEAVE_REQ"
                );
                log.info("✅ [알림] 근태 신청 알림 발송 완료: 에이전시={}, 회원={}", 
                    member.getAgency().getAgencyNo(), member.getMemberNo());
            } else {
                log.warn("⚠️ [알림] 회원의 에이전시가 null: memberNo={}, memberName={}", 
                    member.getMemberNo(), member.getMemberName());
            }
        } catch (Exception e) {
            log.error("❌ [알림] 근태 신청 알림 발송 실패: memberNo={}, agencyNo={}, error={}", 
                member.getMemberNo(), 
                member.getAgency() != null ? member.getAgency().getAgencyNo() : "null",
                e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 근태 신청 승인 시 알림 발송 (비동기)
     * @param request 승인된 근태 신청 정보
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> sendApprovalNotification(AttendanceRequest request) {
        try {
            if (request.getMember() != null) {
                String notificationName = "근태 신청 승인";
                String notificationText = String.format("%s 신청이 승인되었습니다. (%s ~ %s)",
                        request.getAttendanceRequestType(),
                        request.getAttendanceRequestStartDate().toLocalDate(),
                        request.getAttendanceRequestEndDate().toLocalDate());

                notificationService.createNotification(
                        request.getMember().getMemberNo(),
                        notificationName,
                        notificationText,
                        "LEAVE_APP"
                );
            }
        } catch (Exception e) {
            log.error("근태 승인 알림 발송 실패: 신청번호={}", 
                    request.getAttendanceRequestNo(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 근태 신청 반려 시 알림 발송 (비동기)
     * @param request 반려된 근태 신청 정보
     * @param rejectReason 반려 사유
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> sendRejectionNotification(AttendanceRequest request, String rejectReason) {
        try {
            if (request.getMember() != null) {
                String notificationName = "근태 신청 반려";
                String notificationText = String.format("%s 신청이 반려되었습니다. 사유: %s",
                        request.getAttendanceRequestType(),
                        rejectReason);

                notificationService.createNotification(
                        request.getMember().getMemberNo(),
                        notificationName,
                        notificationText,
                        "LEAVE_REJ"
                );
            }
        } catch (Exception e) {
            log.error("근태 반려 알림 발송 실패: 신청번호={}", 
                    request.getAttendanceRequestNo(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
