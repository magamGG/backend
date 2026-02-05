package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.dto.request.AttendanceRequestCreateRequest;
import com.kh.magamGG.domain.attendance.dto.request.LeaveBalanceAdjustRequest;
import com.kh.magamGG.domain.attendance.dto.response.AgencyMemberLeaveResponse;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveBalanceResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveHistoryResponse;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.entity.LeaveHistory;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.attendance.repository.LeaveBalanceRepository;
import com.kh.magamGG.domain.attendance.repository.LeaveHistoryRepository;
import com.kh.magamGG.domain.attendance.repository.ProjectLeaveRequestRepository;
import com.kh.magamGG.domain.attendance.entity.ProjectLeaveRequest;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import com.kh.magamGG.domain.health.dto.request.DailyHealthCheckRequest;
import com.kh.magamGG.domain.health.service.DailyHealthCheckService;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.global.exception.AccountBlockedException;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.AlreadyCheckedInException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.TreeMap;

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
    private final LeaveHistoryRepository leaveHistoryRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;
    private final DailyHealthCheckService dailyHealthCheckService;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final ManagerRepository managerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLeaveRequestRepository projectLeaveRequestRepository;
    // 비즈니스 로직 분리: 연차 차감 서비스
    private final LeaveBalanceDeductionService leaveBalanceDeductionService;
    // 비즈니스 로직 분리: 알림 발송 서비스 (비동기 처리)
    private final AttendanceNotificationService attendanceNotificationService;

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
        
        // 휴재 신청이고 프로젝트 번호가 제공된 경우, PROJECT_LEAVE_REQUEST 저장
        if ("휴재".equals(request.getAttendanceRequestType()) && request.getProjectNo() != null) {
            Project project = projectRepository.findById(request.getProjectNo())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트입니다."));
            
            ProjectLeaveRequest projectLeaveRequest = ProjectLeaveRequest.builder()
                    .attendanceRequest(savedRequest)
                    .project(project)
                    .build();
            
            projectLeaveRequestRepository.save(projectLeaveRequest);
            
            log.info("프로젝트 휴재 신청 생성 완료: 프로젝트 {} ({})", 
                    project.getProjectName(), 
                    request.getProjectNo());
        }
        
        log.info("근태 신청 생성 완료: 회원 {} ({}), 타입: {}, 기간: {} ~ {}", 
                member.getMemberName(), 
                memberNo, 
                request.getAttendanceRequestType(),
                startDate,
                endDate);
        
        // 알림 발송 (비동기 처리)
        attendanceNotificationService.sendAttendanceRequestNotification(
                member,
                request.getAttendanceRequestType(),
                startDate,
                endDate
        );

        // 프로젝트 정보를 포함하여 조회 (N+1 방지)
        AttendanceRequest requestWithProject = attendanceRequestRepository.findByIdWithProject(savedRequest.getAttendanceRequestNo())
                .orElse(savedRequest);
        
        return AttendanceRequestResponse.fromEntity(requestWithProject);
    }
    
    @Override
    public List<AttendanceRequestResponse> getAttendanceRequestsByMember(Long memberNo) {
        // 최적화: JOIN FETCH로 N+1 문제 방지 (Repository에서 이미 처리됨)
        List<AttendanceRequest> requests = attendanceRequestRepository
                .findByMember_MemberNoOrderByAttendanceRequestCreatedAtDesc(memberNo);
        
        if (requests.isEmpty()) {
            // 회원 존재 확인 (데이터가 없을 때만 확인하여 불필요한 쿼리 제거)
            memberRepository.findById(memberNo)
                    .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        }
        
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
    
    /**
     * 에이전시 존재 여부 검증 (없으면 AgencyNotFoundException) — 연차/근태 조회 공통
     */
    private void validateAgencyExists(Long agencyNo) {
        agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
    }

    @Override
    public List<AttendanceRequestResponse> getAttendanceRequestsByAgency(Long agencyNo) {
        validateAgencyExists(agencyNo);
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
        validateAgencyExists(agencyNo);
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
        // member_no 기준 attendance에서 출근한 날짜 목록 조회
        List<LocalDate> checkInDates = attendanceRepository
            .findDistinctCheckInDatesByMemberNoAndMonth(memberNo, year, month)
            .stream()
            .map(java.sql.Date::toLocalDate)
            .collect(Collectors.toList());
        Set<LocalDate> checkInSet = new HashSet<>(checkInDates);
        
        // 해당 회원의 승인된 근태 신청 목록
        List<AttendanceRequest> approvedRequests = attendanceRequestRepository.findApprovedByMemberNo(memberNo);
        
        // 이번 달 1일부터 오늘까지의 모든 날짜 확인
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.isBefore(firstDayOfMonth.plusMonths(1)) ? today : firstDayOfMonth.plusMonths(1).minusDays(1);
        
        Map<String, Long> typeToCount = new TreeMap<>();
        
        // 이번 달 1일부터 오늘까지 순회
        LocalDate currentDate = firstDayOfMonth;
        while (!currentDate.isAfter(endDate)) {
            boolean hasCheckIn = checkInSet.contains(currentDate);
            String type = null;
            
            // 승인된 근태 신청 확인
            for (AttendanceRequest req : approvedRequests) {
                LocalDate start = req.getAttendanceRequestStartDate().toLocalDate();
                LocalDate end = req.getAttendanceRequestEndDate().toLocalDate();
                if (!currentDate.isBefore(start) && !currentDate.isAfter(end)) {
                    type = req.getAttendanceRequestType() != null ? req.getAttendanceRequestType() : "출근";
                    break;
                }
            }
            
            if (hasCheckIn) {
                // 출근한 날: 승인된 신청이 있으면 그 타입, 없으면 "출근"
                String finalType = (type != null) ? type : "출근";
                typeToCount.merge(finalType, 1L, Long::sum);
            } else if (type != null) {
                // 출근하지 않았지만 승인된 근태 신청이 있는 날 (연차, 휴가 등)
                typeToCount.merge(type, 1L, Long::sum);
            }
            // 출근하지 않고 승인된 신청도 없으면 집계하지 않음 (미출근은 프론트에서 계산)
            
            currentDate = currentDate.plusDays(1);
        }
        
        List<AttendanceStatisticsResponseDto.TypeCount> typeCounts = typeToCount.entrySet().stream()
            .map(e -> AttendanceStatisticsResponseDto.TypeCount.builder()
                .type(e.getKey())
                .count(e.getValue())
                .build())
            .collect(Collectors.toList());
        
        // totalCount는 출근한 날짜 수 (기존 로직 유지)
        return AttendanceStatisticsResponseDto.builder()
            .typeCounts(typeCounts)
            .totalCount(checkInDates.size())
            .build();
    }
    
    @Override
    public AttendanceRequestResponse getCurrentAttendanceStatus(Long memberNo) {
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
            // 데이터가 없을 때만 회원 존재 확인하여 불필요한 쿼리 제거
            memberRepository.findById(memberNo)
                    .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
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

        // 연차/반차/반반차인 경우 연차 잔액(remain) 및 사용일수(usedDays) 차감 (차감 실패 시 승인하지 않음)
        String requestType = request.getAttendanceRequestType();
        if ("연차".equals(requestType) || "반차".equals(requestType) || "반반차".equals(requestType)) {
            Long memberNo = request.getMember() != null ? request.getMember().getMemberNo() : null;
            Integer usingDays = request.getAttendanceRequestUsingDays();
            if (memberNo != null) {
                leaveBalanceDeductionService.deductLeaveBalance(memberNo, requestType, usingDays);
            }
        }

        // 승인 처리
        request.approve();
        AttendanceRequest savedRequest = attendanceRequestRepository.save(request);

        log.info("근태 신청 승인 완료: 신청번호={}, 회원={}",
                attendanceRequestNo,
                savedRequest.getMember().getMemberName());

        // 알림 발송 (비동기 처리)
        attendanceNotificationService.sendApprovalNotification(savedRequest);

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

        // 알림 발송 (비동기 처리)
        attendanceNotificationService.sendRejectionNotification(savedRequest, rejectReason);

        return AttendanceRequestResponse.fromEntity(savedRequest);
    }

    @Override
    @Transactional
    public AttendanceRequestResponse cancelAttendanceRequest(Long attendanceRequestNo, Long memberNo) {
        AttendanceRequest request = attendanceRequestRepository.findById(attendanceRequestNo)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 근태 신청입니다."));

        if (!request.getMember().getMemberNo().equals(memberNo)) {
            throw new RuntimeException("본인의 신청만 취소할 수 있습니다.");
        }

        if (!"PENDING".equals(request.getAttendanceRequestStatus()) && !"REJECTED".equals(request.getAttendanceRequestStatus())) {
            throw new RuntimeException("대기 또는 반려 상태의 신청만 취소할 수 있습니다.");
        }

        request.cancel();
        AttendanceRequest savedRequest = attendanceRequestRepository.save(request);

        log.info("근태 신청 취소 완료: 신청번호={}, 회원={}", attendanceRequestNo, memberNo);
        return AttendanceRequestResponse.fromEntity(savedRequest);
    }

    @Override
    @Transactional
    public AttendanceRequestResponse updateAttendanceRequest(Long attendanceRequestNo,
            AttendanceRequestCreateRequest request, Long memberNo) {
        AttendanceRequest entity = attendanceRequestRepository.findById(attendanceRequestNo)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 근태 신청입니다."));

        if (!entity.getMember().getMemberNo().equals(memberNo)) {
            throw new RuntimeException("본인의 신청만 수정할 수 있습니다.");
        }

        if (!"PENDING".equals(entity.getAttendanceRequestStatus())) {
            throw new RuntimeException("대기 상태의 신청만 수정할 수 있습니다.");
        }

        LocalDateTime startDate = parseDateTime(request.getAttendanceRequestStartDate(), false);
        LocalDateTime endDate = parseDateTime(request.getAttendanceRequestEndDate(), true);

        entity.update(
                request.getAttendanceRequestType(),
                startDate,
                endDate,
                request.getAttendanceRequestUsingDays(),
                request.getAttendanceRequestReason(),
                request.getWorkcationLocation(),
                request.getMedicalFileUrl() != null ? request.getMedicalFileUrl() : ""
        );
        AttendanceRequest savedRequest = attendanceRequestRepository.save(entity);

        log.info("근태 신청 수정 완료: 신청번호={}, 회원={}", attendanceRequestNo, memberNo);
        return AttendanceRequestResponse.fromEntity(savedRequest);
    }

    @Override
    public List<LeaveHistoryResponse> getLeaveHistoryByAgency(Long agencyNo) {
        validateAgencyExists(agencyNo);
        return leaveHistoryRepository.findByAgencyNoWithMember(agencyNo).stream()
                .map(LeaveHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgencyMemberLeaveResponse> getLeaveBalancesByAgency(Long agencyNo) {
        validateAgencyExists(agencyNo);
        int currentYear = LocalDateTime.now().getYear();
        List<Object[]> adjustmentSums = leaveHistoryRepository.sumAdjustmentByAgencyNoAndYear(agencyNo, currentYear);
        Map<Long, Integer> adjustmentByMember = adjustmentSums.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).intValue(), (a, b) -> a));

        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<AgencyMemberLeaveResponse> result = new ArrayList<>();
        for (Member m : members) {
            // 직원 연차 관리 목록에는 에이전시 관리자 제외 (담당자·작가 등만 표시)
            if ("에이전시 관리자".equals(m.getMemberRole())) {
                continue;
            }
            Long memberNo = m.getMemberNo();
            int currentYearAdjustmentTotal = adjustmentByMember.getOrDefault(memberNo, 0);

            Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findTop1ByMember_MemberNoOrderByLeaveBalanceYearDesc(memberNo);
            int total = 0;
            int used = 0;
            double remain = 0.0;
            String year = "";
            Long leaveBalanceNo = null;
            if (balanceOpt.isPresent()) {
                LeaveBalance b = balanceOpt.get();
                total = b.getLeaveBalanceTotalDays() != null ? b.getLeaveBalanceTotalDays() : 0;
                used = b.getLeaveBalanceUsedDays() != null ? b.getLeaveBalanceUsedDays() : 0;
                remain = b.getLeaveBalanceRemainDays() != null ? b.getLeaveBalanceRemainDays() : 0.0;
                year = b.getLeaveBalanceYear() != null ? b.getLeaveBalanceYear() : "";
                leaveBalanceNo = b.getLeaveBalanceNo();
            }
            result.add(AgencyMemberLeaveResponse.builder()
                    .memberNo(memberNo)
                    .memberName(m.getMemberName() != null ? m.getMemberName() : "")
                    .memberRole(m.getMemberRole() != null ? m.getMemberRole() : "")
                    .leaveBalanceTotalDays(total)
                    .leaveBalanceUsedDays(used)
                    .leaveBalanceRemainDays(remain)
                    .leaveBalanceYear(year)
                    .leaveBalanceNo(leaveBalanceNo)
                    .currentYearAdjustmentTotal(currentYearAdjustmentTotal)
                    .build());
        }
        return result;
    }

    @Override
    @Cacheable(value = "leaveBalance", key = "#memberNo")
    public LeaveBalanceResponse getLeaveBalance(Long memberNo) {
        // 최적화: 데이터가 없을 때만 회원 존재 확인하여 불필요한 쿼리 제거
        // 캐싱: 자주 조회되는 연차 잔액 정보 캐싱
        return leaveBalanceRepository.findTop1ByMember_MemberNoOrderByLeaveBalanceYearDesc(memberNo)
                .map(LeaveBalanceResponse::fromEntity)
                .orElseGet(() -> {
                    // 데이터가 없을 때만 회원 존재 확인
                    memberRepository.findById(memberNo)
                            .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
                    return null;
                });
    }

    @Override
    @Transactional
    @CacheEvict(value = "leaveBalance", key = "#memberNo")
    public LeaveBalanceResponse adjustLeaveBalance(Long memberNo, LeaveBalanceAdjustRequest request) {
        Member member = memberRepository.findByIdWithAgency(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
        LeaveBalance balance = leaveBalanceRepository.findTop1ByMember_MemberNoOrderByLeaveBalanceYearDesc(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 연차 잔액이 없습니다. 연차를 먼저 부여해주세요."));
        int adjustment = request.getAdjustment() != null ? request.getAdjustment() : 0;
        double currentRemain = balance.getLeaveBalanceRemainDays() != null ? balance.getLeaveBalanceRemainDays() : 0.0;
        double newRemain = currentRemain + adjustment;
        if (newRemain < 0) {
            throw new IllegalArgumentException("조정 후 잔여 연차는 0 미만이 될 수 없습니다.");
        }
        // total은 변경하지 않고 remain만 변경
        balance.setLeaveBalanceRemainDays(newRemain);
        balance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
        leaveBalanceRepository.save(balance);

        LeaveHistory history = new LeaveHistory();
        history.setMember(member);
        history.setLeaveHistoryDate(LocalDateTime.now());
        history.setLeaveHistoryType(request.getReason() != null ? request.getReason() : "");
        history.setLeaveHistoryReason(request.getNote() != null ? request.getNote() : "");
        history.setLeaveHistoryAmount(adjustment);
        leaveHistoryRepository.save(history);

        log.info("연차 조정 완료: 회원번호={}, 사유={}, 조정일수={}, 조정 후 잔여={}", memberNo, request.getReason(), adjustment, newRemain);
        // 응답 시 balance.getMember() lazy load 방지: 이미 가진 값으로 DTO 생성
        return LeaveBalanceResponse.builder()
                .leaveBalanceNo(balance.getLeaveBalanceNo())
                .memberNo(memberNo)
                .leaveBalanceTotalDays(balance.getLeaveBalanceTotalDays() != null ? balance.getLeaveBalanceTotalDays() : 0)
                .leaveBalanceUsedDays(balance.getLeaveBalanceUsedDays() != null ? balance.getLeaveBalanceUsedDays() : 0)
                .leaveBalanceRemainDays(newRemain)
                .leaveBalanceYear(balance.getLeaveBalanceYear() != null ? balance.getLeaveBalanceYear() : "")
                .build();
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

    @Override
    public List<AttendanceRequestResponse> getWeeklyAttendanceByManager(Long memberNo) {
        Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
        if (managerOpt.isEmpty()) {
            log.debug("담당자 아님: memberNo={}", memberNo);
            return List.of();
        }

        Set<Long> artistMemberNos = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo())
                .stream()
                .map(a -> a.getArtist().getMemberNo())
                .collect(Collectors.toSet());

        if (artistMemberNos.isEmpty()) {
            return List.of();
        }

        Long agencyNo = Optional.ofNullable(managerOpt.get().getMember().getAgency())
                .map(a -> a.getAgencyNo())
                .orElse(null);
        if (agencyNo == null) {
            agencyNo = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo()).stream()
                    .map(a -> a.getArtist().getAgency())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(a -> a.getAgencyNo())
                    .orElse(null);
        }
        if (agencyNo == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        List<AttendanceRequest> agencyRequests = attendanceRequestRepository.findByAgencyNoWithMember(agencyNo);

        if (agencyRequests == null || agencyRequests.isEmpty()) {
            return List.of();
        }

        return agencyRequests.stream()
                .filter(req -> !"CANCELLED".equals(req.getAttendanceRequestStatus()))
                .filter(req -> artistMemberNos.contains(req.getMember().getMemberNo()))
                .filter(req -> {
                    LocalDate reqStart = req.getAttendanceRequestStartDate().toLocalDate();
                    LocalDate reqEnd = req.getAttendanceRequestEndDate().toLocalDate();
                    return !reqStart.isAfter(sunday) && !reqEnd.isBefore(monday);
                })
                .sorted((a, b) -> a.getAttendanceRequestStartDate().compareTo(b.getAttendanceRequestStartDate()))
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceRequestResponse> getAttendanceRequestsByManager(Long memberNo) {
        Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
        if (managerOpt.isEmpty()) {
            log.debug("담당자 아님: memberNo={}", memberNo);
            return List.of();
        }

        // 담당자에게 배정된 작가들의 memberNo 집합
        Set<Long> artistMemberNos = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo())
                .stream()
                .map(a -> a.getArtist().getMemberNo())
                .collect(Collectors.toSet());

        if (artistMemberNos.isEmpty()) {
            return List.of();
        }

        // 담당자의 에이전시 번호 추론 (없으면 담당 작가 중 한 명의 에이전시 사용)
        Long agencyNo = Optional.ofNullable(managerOpt.get().getMember().getAgency())
                .map(a -> a.getAgencyNo())
                .orElse(null);
        if (agencyNo == null) {
            agencyNo = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo()).stream()
                    .map(a -> a.getArtist().getAgency())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(a -> a.getAgencyNo())
                    .orElse(null);
        }
        if (agencyNo == null) {
            return List.of();
        }

        // 에이전시 기준으로 조회한 뒤, 담당 작가들만 필터링
        List<AttendanceRequest> agencyRequests = attendanceRequestRepository.findByAgencyNoWithMember(agencyNo);
        if (agencyRequests == null || agencyRequests.isEmpty()) {
            return List.of();
        }

        return agencyRequests.stream()
                .filter(req -> !"CANCELLED".equals(req.getAttendanceRequestStatus()))
                .filter(req -> artistMemberNos.contains(req.getMember().getMemberNo()))
                .sorted((a, b) -> b.getAttendanceRequestCreatedAt().compareTo(a.getAttendanceRequestCreatedAt()))
                .map(AttendanceRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
