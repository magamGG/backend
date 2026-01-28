package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 근태 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {
    
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;
    
    @Override
    @Transactional
    public AttendanceRequestResponse createAttendanceRequest(AttendanceRequestCreateRequest request, Long memberNo) {
        // 회원 조회 (Agency 정보도 함께 조회하여 알림 발송 시 사용)
        Member member = memberRepository.findByIdWithAgency(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        
        // 날짜 파싱 (ISO 8601 형식 지원)
        // 시작일은 00:00:00, 종료일은 23:59:59로 설정
        LocalDateTime startDate = parseDateTime(request.getAttendanceRequestStartDate(), false);
        LocalDateTime endDate = parseDateTime(request.getAttendanceRequestEndDate(), true);
        
        // AttendanceRequest 엔티티 생성
        AttendanceRequest attendanceRequest = AttendanceRequest.builder()
                .member(member)
                .attendanceRequestType(request.getAttendanceRequestType())
                .attendanceRequestStartDate(startDate)
                .attendanceRequestEndDate(endDate)
                .attendanceRequestUsingDays(request.getAttendanceRequestUsingDays())
                .attendanceRequestReason(request.getAttendanceRequestReason())
                .workcationLocation(request.getWorkcationLocation())
                .medicalFileUrl(request.getMedicalFileUrl() != null ? request.getMedicalFileUrl() : "")
                .attendanceRequestStatus("PENDING")
                .attendanceRequestCreatedAt(LocalDateTime.now())
                .build();
        
        // 저장
        AttendanceRequest savedRequest = attendanceRequestRepository.save(attendanceRequest);
        
        log.info("근태 신청 생성 완료: 회원 {} ({}), 타입: {}, 기간: {} ~ {}", 
                member.getMemberName(), 
                memberNo, 
                request.getAttendanceRequestType(),
                request.getAttendanceRequestStartDate(),
                request.getAttendanceRequestEndDate());
        
        // 에이전시 담당자에게 알림 발송
        if (member.getAgency() != null) {
            String notificationName = "근태 신청";
            String notificationText = String.format("%s님이 %s을(를) 신청했습니다. (%s ~ %s)", 
                    member.getMemberName(),
                    request.getAttendanceRequestType(),
                    request.getAttendanceRequestStartDate(),
                    request.getAttendanceRequestEndDate());
            
            notificationService.notifyAgencyManagers(
                    member.getAgency().getAgencyNo(),
                    notificationName,
                    notificationText,
                    "LEAVE_REQ"
            );
        }
        
        return AttendanceRequestResponse.fromEntity(savedRequest);
    }
    
    @Override
    public List<AttendanceRequestResponse> getAttendanceRequestsByMember(Long memberNo) {
        // 회원 존재 확인
        memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        
        List<AttendanceRequest> requests = attendanceRequestRepository
                .findByMember_MemberNoOrderByAttendanceRequestCreatedAtDesc(memberNo);
        
        return requests.stream()
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AttendanceRequestResponse> getPendingAttendanceRequests() {
        List<AttendanceRequest> requests = attendanceRequestRepository
                .findByAttendanceRequestStatusOrderByAttendanceRequestCreatedAtDesc("PENDING");
        
        return requests.stream()
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AttendanceRequestResponse> getAttendanceRequestsByAgency(Long agencyNo) {
        // 에이전시 존재 확인
        agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
        
        // JOIN FETCH로 N+1 문제 방지
        List<AttendanceRequest> requests = attendanceRequestRepository
                .findByAgencyNoWithMember(agencyNo);
        
        log.info("에이전시 {} 소속 근태 신청 조회: {}건", agencyNo, requests.size());
        
        return requests.stream()
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AttendanceRequestResponse> getPendingAttendanceRequestsByAgency(Long agencyNo) {
        // 에이전시 존재 확인
        agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
        
        // JOIN FETCH로 N+1 문제 방지 + 상태 필터링
        List<AttendanceRequest> requests = attendanceRequestRepository
                .findByAgencyNoAndStatusWithMember(agencyNo, "PENDING");
        
        log.info("에이전시 {} 소속 대기 중인 근태 신청 조회: {}건", agencyNo, requests.size());
        
        return requests.stream()
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 문자열을 LocalDateTime으로 파싱
     * YYYY-MM-DD 또는 YYYY-MM-DDTHH:mm:ss 형식 지원
     * 
     * @param dateTimeStr 날짜 문자열
     * @param isEndDate 종료일 여부 (true: 23:59:59, false: 00:00:00)
     */
    private LocalDateTime parseDateTime(String dateTimeStr, boolean isEndDate) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // ISO 8601 형식 (YYYY-MM-DDTHH:mm:ss) - 이미 시간이 포함된 경우 그대로 사용
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            // 날짜만 있는 경우 (YYYY-MM-DD) - 시작일은 00:00:00, 종료일은 23:59:59
            String timeStr = isEndDate ? "T23:59:59" : "T00:00:00";
            return LocalDateTime.parse(dateTimeStr + timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 기본값 사용", dateTimeStr);
            return LocalDateTime.now();
        }
    }
}
