package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.health.dto.request.DailyHealthCheckRequest;
import com.kh.magamGG.domain.health.service.DailyHealthCheckService;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.global.exception.AccountBlockedException;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.AlreadyCheckedInException;
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
    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;
    private final DailyHealthCheckService dailyHealthCheckService;
    private final ArtistAssignmentRepository artistAssignmentRepository;

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
        // 작가의 담당자에게만 알림 발송 (ARTIST_ASSIGNMENT 테이블에서 조회)
        artistAssignmentRepository.findByArtistMemberNo(memberNo)
                .ifPresent(assignment -> {
                    // 담당자의 MEMBER_NO 조회
                    Long managerMemberNo = assignment.getManager().getMember().getMemberNo();

                    String notificationName = "근태 신청";
                    String notificationText = String.format("%s님이 %s을(를) 신청했습니다. (%s ~ %s)",
                            member.getMemberName(),
                            request.getAttendanceRequestType(),
                            request.getAttendanceRequestStartDate(),
                            request.getAttendanceRequestEndDate());

                    // 담당자에게만 알림 발송
                    notificationService.createNotification(
                            managerMemberNo,
                            notificationName,
                            notificationText,
                            "LEAVE_REQ"
                    );

                    log.info("근태 신청 알림 발송: 작가={}, 담당자={}", memberNo, managerMemberNo);
                });

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
    
    @Override
    public AttendanceStatisticsResponseDto getAttendanceStatistics(Long memberNo, int year, int month) {
        List<Object[]> results = attendanceRepository.countByMemberNoAndMonth(memberNo, year, month);
        
        List<AttendanceStatisticsResponseDto.TypeCount> typeCounts = results.stream()
            .map(result -> AttendanceStatisticsResponseDto.TypeCount.builder()
                .type((String) result[0])
                .count((Long) result[1])
                .build())
            .collect(Collectors.toList());
        
        Integer totalCount = typeCounts.stream()
            .mapToInt(count -> count.getCount().intValue())
            .sum();
        
        return AttendanceStatisticsResponseDto.builder()
            .typeCounts(typeCounts)
            .totalCount(totalCount)
            .build();
    }
    
    @Override
    public AttendanceRequestResponse getCurrentAttendanceStatus(Long memberNo) {
        // 회원 존재 확인
        memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 현재 날짜만 추출 (시간 무시)
        LocalDateTime now = LocalDateTime.now();
        java.time.LocalDate today = now.toLocalDate();

        log.info("회원 {}의 현재 근태 상태 조회 시작 - 오늘 날짜: {}", memberNo, today);

        // 승인된 근태 신청 목록 조회
        List<AttendanceRequest> approvedRequests = attendanceRequestRepository
                .findApprovedByMemberNo(memberNo);

        log.info("회원 {}의 승인된 근태 신청 수: {}", memberNo, approvedRequests.size());

        // 현재 날짜가 시작일과 종료일 사이에 있는 것만 필터링
        List<AttendanceRequest> currentRequests = approvedRequests.stream()
                .filter(request -> {
                    java.time.LocalDate startDate = request.getAttendanceRequestStartDate().toLocalDate();
                    java.time.LocalDate endDate = request.getAttendanceRequestEndDate().toLocalDate();
                    boolean isInRange = !today.isBefore(startDate) && !today.isAfter(endDate);
                    log.info("근태 신청 {}: 시작일={}, 종료일={}, 오늘={}, 범위 내={}",
                            request.getAttendanceRequestNo(), startDate, endDate, today, isInRange);
                    return isInRange;
                })
                .collect(Collectors.toList());

        if (currentRequests.isEmpty()) {
            log.info("회원 {}의 현재 적용 중인 근태 상태 없음 (승인된 신청 {}건 중 현재 날짜 범위 내 신청 없음)",
                    memberNo, approvedRequests.size());
            return null;
        }

        // 가장 최근 승인된 것 반환 (여러 개일 경우)
        AttendanceRequest currentRequest = currentRequests.get(0);
        log.info("회원 {}의 현재 근태 상태: {} ({} ~ {})",
                memberNo,
                currentRequest.getAttendanceRequestType(),
                currentRequest.getAttendanceRequestStartDate(),
                currentRequest.getAttendanceRequestEndDate());

        return AttendanceRequestResponse.fromEntity(currentRequest);
    }

    @Override
    @Transactional
    public AttendanceRequestResponse approveAttendanceRequest(Long attendanceRequestNo) {
        // 근태 신청 조회
        AttendanceRequest request = attendanceRequestRepository.findById(attendanceRequestNo)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 근태 신청입니다."));

        // 이미 처리된 경우 예외 처리
        if (!"PENDING".equals(request.getAttendanceRequestStatus())) {
            throw new RuntimeException("이미 처리된 근태 신청입니다.");
        }

        // 승인 처리
        request.approve();
        AttendanceRequest savedRequest = attendanceRequestRepository.save(request);

        log.info("근태 신청 승인 완료: 신청번호={}, 회원={}",
                attendanceRequestNo,
                savedRequest.getMember().getMemberName());

        // 신청자에게 알림 발송
        if (savedRequest.getMember() != null) {
            String notificationName = "근태 신청 승인";
            String notificationText = String.format("%s 신청이 승인되었습니다. (%s ~ %s)",
                    savedRequest.getAttendanceRequestType(),
                    savedRequest.getAttendanceRequestStartDate().toLocalDate(),
                    savedRequest.getAttendanceRequestEndDate().toLocalDate());

            notificationService.createNotification(
                    savedRequest.getMember().getMemberNo(),
                    notificationName,
                    notificationText,
                    "LEAVE_APP"
            );
        }

        return AttendanceRequestResponse.fromEntity(savedRequest);
    }

    @Override
    @Transactional
    public AttendanceRequestResponse rejectAttendanceRequest(Long attendanceRequestNo, String rejectReason) {
        // 근태 신청 조회
        AttendanceRequest request = attendanceRequestRepository.findById(attendanceRequestNo)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 근태 신청입니다."));

        // 이미 처리된 경우 예외 처리
        if (!"PENDING".equals(request.getAttendanceRequestStatus())) {
            throw new RuntimeException("이미 처리된 근태 신청입니다.");
        }

        // 반려 처리
        request.reject(rejectReason);
        AttendanceRequest savedRequest = attendanceRequestRepository.save(request);

        log.info("근태 신청 반려 완료: 신청번호={}, 회원={}, 사유={}",
                attendanceRequestNo,
                savedRequest.getMember().getMemberName(),
                rejectReason);

        // 신청자에게 알림 발송
        if (savedRequest.getMember() != null) {
            String notificationName = "근태 신청 반려";
            String notificationText = String.format("%s 신청이 반려되었습니다. 사유: %s",
                    savedRequest.getAttendanceRequestType(),
                    rejectReason);

            notificationService.createNotification(
                    savedRequest.getMember().getMemberNo(),
                    notificationName,
                    notificationText,
                    "LEAVE_REJ"
            );
        }

        return AttendanceRequestResponse.fromEntity(savedRequest);
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

    @Override
    @Transactional
    public boolean startAttendance(DailyHealthCheckRequest healthCheckRequest, Long memberNo) {
        // 1. 회원 조회 및 검증
        Member member = memberRepository.findByIdWithAgency(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원 정보를 찾을 수 없습니다."));

        if (member.getAgency() == null) {
            throw new MemberNotFoundException("에이전시에 소속되지 않은 회원입니다.");
        }

        // 2. 회원 상태 확인 (ACTIVE만 출근 가능)
        if (member.getMemberStatus() == null || !"ACTIVE".equals(member.getMemberStatus())) {
            throw new AccountBlockedException("휴면 계정입니다.");
        }

        // 3. 오늘 이미 출근했는지 확인
        String lastType = getTodayLastAttendanceType(memberNo);
        if ("출근".equals(lastType)) {
            throw new AlreadyCheckedInException("이미 출근 처리되었습니다.");
        }

        // 4. DAILY_HEALTH_CHECK 저장 (건강 체크)
        dailyHealthCheckService.createDailyHealthCheck(healthCheckRequest, memberNo);

        // 5. ATTENDANCE 테이블에 출근 기록 생성
        Attendance attendance = new Attendance();
        attendance.setMember(member);
        attendance.setAgency(member.getAgency());
        attendance.setAttendanceType("출근");
        attendance.setAttendanceTime(LocalDateTime.now());
        attendanceRepository.save(attendance);

        log.info("출근 시작 완료: 회원번호={}, 에이전시번호={}, ATTENDANCE 저장됨", memberNo, member.getAgency().getAgencyNo());

        return true;
    }

    @Override
    public String getTodayLastAttendanceType(Long memberNo) {
        // 회원 존재 확인
        memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 오늘 날짜
        java.time.LocalDate today = java.time.LocalDate.now();

        // 오늘 날짜의 마지막 출근 기록 조회
        List<Attendance> todayAttendances = attendanceRepository.findTodayLastAttendanceByMemberNo(memberNo, today);

        if (todayAttendances.isEmpty()) {
            return null;
        }

        // 가장 최근 기록의 타입 반환
        Attendance lastAttendance = todayAttendances.get(0);
        return lastAttendance.getAttendanceType();
    }

    @Override
    @Transactional
    public boolean endAttendance(Long memberNo) {
        Member member = memberRepository.findByIdWithAgency(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        if (member.getAgency() == null) {
            throw new MemberNotFoundException("에이전시에 소속되지 않은 회원입니다.");
        }
        Attendance attendance = new Attendance();
        attendance.setMember(member);
        attendance.setAgency(member.getAgency());
        attendance.setAttendanceType("퇴근");
        attendance.setAttendanceTime(LocalDateTime.now());
        attendanceRepository.save(attendance);
        log.info("출근 종료(퇴근) 완료: 회원번호={}", memberNo);
        return true;
    }
}
