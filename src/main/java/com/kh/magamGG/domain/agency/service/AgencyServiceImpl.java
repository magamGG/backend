package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.UpdateHealthScheduleRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDeadlineCountResponse;
import com.kh.magamGG.domain.agency.dto.response.ArtistDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.AttendanceDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyHealthScheduleResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyUnscreenedListResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthMonitoringDetailResponse;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.attendance.entity.Attendance;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.attendance.repository.LeaveBalanceRepository;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.NewRequest;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.repository.NewRequestRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.domain.health.dto.HealthSurveyRiskLevelDto;
import com.kh.magamGG.domain.health.entity.HealthSurvey;
import com.kh.magamGG.domain.health.entity.HealthSurveyResponseItem;
import com.kh.magamGG.domain.health.repository.HealthSurveyRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyResponseItemRepository;
import com.kh.magamGG.domain.health.service.HealthSurveyService;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import com.kh.magamGG.global.exception.NewRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository agencyRepository;
    private final MemberRepository memberRepository;
    private final NewRequestRepository newRequestRepository;
    private final NotificationService notificationService;
    private final ManagerRepository managerRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final ProjectRepository projectRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final HealthSurveyResponseItemRepository healthSurveyResponseItemRepository;
    private final HealthSurveyService healthSurveyService;
    private final HealthSurveyRepository healthSurveyRepository;

    @Override
    @Transactional
    public JoinRequestResponse createJoinRequest(JoinRequestRequest request, Long memberNo) {
        // 에이전시 코드로 에이전시 조회
        Agency agency = agencyRepository.findByAgencyCode(request.getAgencyCode())
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));

        // 회원 조회
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

        // 이미 해당 에이전시에 가입 요청이 있는지 확인 (대기 상태인 경우)
        List<NewRequest> existingRequests = newRequestRepository.findByMember_MemberNo(memberNo);
        boolean hasPendingRequest = existingRequests.stream()
                .anyMatch(nr -> nr.getAgency().getAgencyNo().equals(agency.getAgencyNo())
                        && "대기".equals(nr.getNewRequestStatus()));

        if (hasPendingRequest) {
            throw new IllegalArgumentException("이미 해당 에이전시에 가입 요청이 대기 중입니다.");
        }

        // NEW_REQUEST 생성
        NewRequest newRequest = NewRequest.builder()
                .agency(agency)
                .member(member)
                .newRequestDate(LocalDateTime.now())
                .newRequestStatus("대기")
                .build();

        newRequest = newRequestRepository.save(newRequest);
        log.info("에이전시 가입 요청 생성: 회원 {} -> 에이전시 {} (요청번호: {})",
                member.getMemberName(), agency.getAgencyName(), newRequest.getNewRequestNo());

        // 에이전시 담당자에게 알림 발송
        String notificationName = "가입 요청";
        String notificationText = String.format("%s님(%s)이 에이전시 가입을 요청했습니다.", 
                member.getMemberName(),
                member.getMemberRole());
        
        notificationService.notifyAgencyManagers(
                agency.getAgencyNo(),
                notificationName,
                notificationText,
                "JOIN_REQ"
        );

        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(agency.getAgencyNo())
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .memberRole(member.getMemberRole())
                .newRequestDate(newRequest.getNewRequestDate())
                .newRequestStatus(newRequest.getNewRequestStatus())
                .build();
    }

    @Override
    public List<JoinRequestResponse> getJoinRequests(Long agencyNo) {
        // 에이전시 존재 확인
        Agency agency = agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));

        // 해당 에이전시의 모든 가입 요청 조회 (최신순)
        List<NewRequest> requests = newRequestRepository.findByAgency_AgencyNoOrderByNewRequestDateDesc(agencyNo);

        return requests.stream()
                .map(nr -> JoinRequestResponse.builder()
                        .newRequestNo(nr.getNewRequestNo())
                        .agencyNo(nr.getAgency().getAgencyNo())
                        .memberNo(nr.getMember().getMemberNo())
                        .memberName(nr.getMember().getMemberName())
                        .memberEmail(nr.getMember().getMemberEmail())
                        .memberPhone(nr.getMember().getMemberPhone())
                        .memberRole(nr.getMember().getMemberRole())
                        .newRequestDate(nr.getNewRequestDate())
                        .newRequestStatus(nr.getNewRequestStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JoinRequestResponse approveJoinRequest(Long newRequestNo) {
        // 가입 요청 조회
        NewRequest newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));

        // 상태 확인 (대기 상태만 승인 가능)
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalArgumentException("이미 처리된 요청입니다. (현재 상태: " + newRequest.getNewRequestStatus() + ")");
        }

        Long memberNo = newRequest.getMember().getMemberNo();
        Long agencyNo = newRequest.getAgency().getAgencyNo();
        String memberName = newRequest.getMember().getMemberName();
        String agencyName = newRequest.getAgency().getAgencyName();

        // 1. NEW_REQUEST 상태를 "승인"으로 변경
        newRequest.setNewRequestStatus("승인");
        newRequestRepository.save(newRequest);
        log.info("NEW_REQUEST 상태 업데이트 완료: {} -> 승인", newRequestNo);

        // 2. MEMBER의 AGENCY_NO 업데이트
        Member memberToUpdate = newRequest.getMember();
        memberToUpdate.setAgency(newRequest.getAgency());
        memberRepository.save(memberToUpdate);
        log.info("MEMBER AGENCY_NO 업데이트 완료: 회원 {} -> 에이전시 {}", memberNo, agencyNo);

        // 3. LEAVE_BALANCE: 같은 memberNo + 연도 있으면 덮어쓰기, 없으면 신규 생성
        int totalDays = newRequest.getAgency().getAgencyLeave() != null
                ? newRequest.getAgency().getAgencyLeave() : 15;
        String currentYear = String.valueOf(java.time.Year.now().getValue());
        Member memberForBalance = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        LeaveBalance leaveBalance = leaveBalanceRepository
                .findByMember_MemberNoAndLeaveBalanceYear(memberNo, currentYear)
                .orElse(null);
        if (leaveBalance != null) {
            leaveBalance.setLeaveBalanceTotalDays(totalDays);
            leaveBalance.setLeaveBalanceUsedDays(0);
            leaveBalance.setLeaveBalanceRemainDays((double) totalDays);
            leaveBalance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
            leaveBalanceRepository.save(leaveBalance);
            log.info("LEAVE_BALANCE 덮어쓰기 완료: 회원번호 {}, 총연차일 {}, 연도 {}", memberNo, totalDays, currentYear);
        } else {
            leaveBalance = new LeaveBalance();
            leaveBalance.setMember(memberForBalance);
            leaveBalance.setLeaveType("ANNUAL");
            leaveBalance.setLeaveBalanceTotalDays(totalDays);
            leaveBalance.setLeaveBalanceUsedDays(0);
            leaveBalance.setLeaveBalanceRemainDays((double) totalDays);
            leaveBalance.setLeaveBalanceYear(currentYear);
            leaveBalance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
            leaveBalanceRepository.save(leaveBalance);
            log.info("LEAVE_BALANCE 초기 데이터 생성 완료: 회원번호 {}, 총연차일 {}, 연도 {}", memberNo, totalDays, currentYear);
        }

        // 4. 담당자인 경우 MANAGER 테이블에 등록 (작가 배정 기능을 위해)
        String memberRole = newRequest.getMember().getMemberRole();
        if ("담당자".equals(memberRole)) {
            // 이미 Manager로 등록되어 있는지 확인
            boolean alreadyManager = managerRepository.findByMember_MemberNo(memberNo).isPresent();
            if (!alreadyManager) {
                Member memberEntity = memberRepository.findById(memberNo)
                        .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
                
                Manager manager = Manager.builder()
                        .member(memberEntity)
                        .build();
                managerRepository.save(manager);
                log.info("MANAGER 테이블에 담당자 등록 완료: 회원번호 {}, 회원명 {}", memberNo, memberName);
            }
        }

        log.info("에이전시 가입 요청 승인 완료: 요청번호 {}, 회원 {} -> 에이전시 {} 소속으로 변경",
                newRequestNo, memberName, agencyName);

        // 업데이트된 엔티티 다시 조회해서 반환
        newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("업데이트 후 요청을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("업데이트 후 회원을 찾을 수 없습니다."));

        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(agencyNo)
                .memberNo(member.getMemberNo())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .memberRole(member.getMemberRole())
                .newRequestDate(newRequest.getNewRequestDate())
                .newRequestStatus(newRequest.getNewRequestStatus())
                .build();
    }

    @Override
    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long newRequestNo, String rejectionReason) {
        // 가입 요청 조회
        NewRequest newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("존재하지 않는 가입 요청입니다."));

        // 상태 확인 (대기 상태만 거절 가능)
        if (!"대기".equals(newRequest.getNewRequestStatus())) {
            throw new IllegalArgumentException("이미 처리된 요청입니다. (현재 상태: " + newRequest.getNewRequestStatus() + ")");
        }

        String memberName = newRequest.getMember().getMemberName();

        // 가입 요청 상태를 "거절"로 변경
        newRequest.setNewRequestStatus("거절");
        newRequestRepository.save(newRequest);

        log.info("에이전시 가입 요청 거절: 요청번호 {}, 회원 {}, 사유: {}",
                newRequestNo, memberName, rejectionReason);

        // 업데이트된 엔티티 다시 조회
        newRequest = newRequestRepository.findById(newRequestNo)
                .orElseThrow(() -> new NewRequestNotFoundException("업데이트 후 요청을 찾을 수 없습니다."));

        return JoinRequestResponse.builder()
                .newRequestNo(newRequest.getNewRequestNo())
                .agencyNo(newRequest.getAgency().getAgencyNo())
                .memberNo(newRequest.getMember().getMemberNo())
                .memberName(newRequest.getMember().getMemberName())
                .memberEmail(newRequest.getMember().getMemberEmail())
                .memberPhone(newRequest.getMember().getMemberPhone())
                .memberRole(newRequest.getMember().getMemberRole())
                .newRequestDate(newRequest.getNewRequestDate())
                .newRequestStatus(newRequest.getNewRequestStatus())
                .build();
    }

    /**
     * 에이전시 조회 (없으면 AgencyNotFoundException) — 연차 관리 등 공통 사용
     */
    private Agency findAgencyOrThrow(Long agencyNo) {
        return agencyRepository.findById(agencyNo)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시입니다."));
    }

    @Override
    public Agency getAgency(Long agencyNo) {
        return findAgencyOrThrow(agencyNo);
    }

    @Override
    public Agency getAgencyByCode(String agencyCode) {
        return agencyRepository.findByAgencyCode(agencyCode)
                .orElseThrow(() -> new AgencyNotFoundException("존재하지 않는 에이전시 코드입니다."));
    }

    @Override
    @Transactional
    public void updateAgencyName(Long agencyNo, String agencyName) {
        Agency agency = findAgencyOrThrow(agencyNo);
        agency.updateAgencyName(agencyName);
        agencyRepository.save(agency);
    }

    @Override
    @Transactional
    public void updateAgencyLeave(Long agencyNo, Integer agencyLeave) {
        if (agencyLeave == null || agencyLeave < 0) {
            throw new IllegalArgumentException("기본 연차는 0 이상이어야 합니다.");
        }
        Agency agency = findAgencyOrThrow(agencyNo);
        agency.updateAgencyLeave(agencyLeave);
        agencyRepository.save(agency);

        // 해당 에이전시 소속 회원들의 당해 연도 LeaveBalance.leaveBalanceTotalDays 동일 값으로 갱신
        String currentYear = String.valueOf(java.time.Year.now().getValue());
        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);
        for (Member member : members) {
            leaveBalanceRepository.findByMember_MemberNoAndLeaveBalanceYear(member.getMemberNo(), currentYear)
            .ifPresent(balance -> {
                balance.setLeaveBalanceTotalDays(agencyLeave);
                int used = balance.getLeaveBalanceUsedDays() != null ? balance.getLeaveBalanceUsedDays() : 0;
                double newRemain = agencyLeave - used;
                balance.setLeaveBalanceRemainDays(newRemain >= 0 ? newRemain : 0.0);
                balance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
                leaveBalanceRepository.save(balance);
                log.info("LeaveBalance 갱신: 회원번호={}, totalDays={}, remainDays={}", member.getMemberNo(), agencyLeave, balance.getLeaveBalanceRemainDays());
            });
        }
    }

    @Override
    public AgencyDashboardMetricsResponse getDashboardMetrics(Long agencyNo) {
        findAgencyOrThrow(agencyNo);

        // 활동 작가: 에이전시 소속 웹툰/웹소설 작가 (ACTIVE 또는 status 미설정)
        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        long activeArtistCount = artists.stream()
                .filter(m -> m.getMemberStatus() == null || "ACTIVE".equals(m.getMemberStatus()))
                .count();

        // 진행 프로젝트: 에이전시 소속 회원이 참여한 연재 중 프로젝트
        long activeProjectCount = projectRepository.findActiveProjectsByAgencyNo(agencyNo).size();

        // 평균 마감 준수율: 에이전시 프로젝트의 KANBAN_CARD 기준
        List<com.kh.magamGG.domain.project.entity.Project> allAgencyProjects =
                projectRepository.findAllProjectsByAgencyNo(agencyNo);

        LocalDate today = LocalDate.now();
        long totalPastDeadline = 0;
        long completedOnTime = 0;
        for (var project : allAgencyProjects) {
            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            for (KanbanCard card : cards) {
                if (card.getKanbanCardEndedAt() == null) continue;
                if (card.getKanbanCardEndedAt().isAfter(today)) continue;
                totalPastDeadline++;
                if ("Y".equals(card.getKanbanCardStatus())) {
                    completedOnTime++;
                }
            }
        }
        double complianceRate = totalPastDeadline > 0
                ? (completedOnTime * 100.0 / totalPastDeadline)
                : 100.0;

        return AgencyDashboardMetricsResponse.builder()
                .averageDeadlineComplianceRate(Math.round(complianceRate * 10) / 10.0)
                .activeArtistCount(activeArtistCount)
                .activeProjectCount(activeProjectCount)
                .complianceRateChange(null)
                .activeArtistChange(null)
                .activeProjectChange(null)
                .build();
    }

    @Override
    public ComplianceTrendResponse getComplianceTrend(Long agencyNo) {
        findAgencyOrThrow(agencyNo);

        List<com.kh.magamGG.domain.project.entity.Project> projects =
                projectRepository.findAllProjectsByAgencyNo(agencyNo);

        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.from(today);
        int monthsCount = 6;

        Map<YearMonth, long[]> monthStats = new LinkedHashMap<>();
        for (int i = monthsCount - 1; i >= 0; i--) {
            monthStats.put(now.minusMonths(i), new long[]{0, 0});
        }

        for (var project : projects) {
            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            for (KanbanCard card : cards) {
                if (card.getKanbanCardEndedAt() == null) continue;
                YearMonth cardMonth = YearMonth.from(card.getKanbanCardEndedAt());
                if (!monthStats.containsKey(cardMonth)) continue;
                if (card.getKanbanCardEndedAt().isAfter(today)) continue;

                long[] stats = monthStats.get(cardMonth);
                stats[0]++;
                if ("Y".equals(card.getKanbanCardStatus())) {
                    stats[1]++;
                }
            }
        }

        List<ComplianceTrendResponse.ComplianceMonthItem> trend = new ArrayList<>();
        Double monthOverMonthChange = null;
        Double prevRate = null;

        for (Map.Entry<YearMonth, long[]> e : monthStats.entrySet()) {
            long total = e.getValue()[0];
            long completed = e.getValue()[1];
            double rate = total > 0 ? Math.round((completed * 100.0 / total) * 10) / 10.0 : 100.0;
            String monthLabel = e.getKey().getMonthValue() + "월";
            trend.add(new ComplianceTrendResponse.ComplianceMonthItem(monthLabel, rate));

            if (prevRate != null) {
                monthOverMonthChange = Math.round((rate - prevRate) * 10) / 10.0;
            }
            prevRate = rate;
        }

        return ComplianceTrendResponse.builder()
                .trend(trend)
                .monthOverMonthChange(monthOverMonthChange)
                .build();
    }

    @Override
    public ArtistDistributionResponse getArtistDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        List<Object[]> rows;
        try {
            rows = projectMemberRepository.countNonManagerMembersByProjectAndAgency(agencyNo);
        } catch (Exception e) {
            log.warn("작품별 아티스트 분포 조회 중 오류: {}", e.getMessage());
            return ArtistDistributionResponse.builder()
                    .distribution(Collections.emptyList())
                    .maxArtistProjectName(null)
                    .build();
        }
        if (rows == null || rows.isEmpty()) {
            return ArtistDistributionResponse.builder()
                    .distribution(Collections.emptyList())
                    .maxArtistProjectName(null)
                    .build();
        }
        List<ArtistDistributionResponse.ArtistDistributionItem> distribution = new ArrayList<>();
        String maxArtistProjectName = null;
        long maxArtists = 0;
        for (Object[] row : rows) {
            String projectName = row[0] != null ? String.valueOf(row[0]) : "-";
            Number count = row[1] instanceof Number ? (Number) row[1] : null;
            long artistsCount = count != null ? count.longValue() : 0L;
            distribution.add(ArtistDistributionResponse.ArtistDistributionItem.builder()
                    .name(projectName)
                    .artists(artistsCount)
                    .build());
            if (artistsCount > maxArtists) {
                maxArtists = artistsCount;
                maxArtistProjectName = projectName;
            }
        }
        return ArtistDistributionResponse.builder()
                .distribution(distribution)
                .maxArtistProjectName(maxArtistProjectName)
                .build();
    }

    /** 근태 신청 타입 → 금일 출석 분포 표시용 (휴가/재택근무/워케이션만, 그 외 null) */
    private static String requestTypeToDisplay(String requestType) {
        if (requestType == null) return null;
        String t = requestType.trim();
        if ("연차".equals(t) || "반차".equals(t) || "반반차".equals(t) || "병가".equals(t) || "휴재".equals(t) || "휴가".equals(t)) {
            return "휴가";
        }
        if ("재택근무".equals(t) || "재택".equals(t)) return "재택근무";
        if ("워케이션".equals(t)) return "워케이션";
        return null;
    }

    /** 금일 출석 분포에서 제외할 역할 (에이전시 관리자·관리자는 직원 현황에 포함하지 않음) */
    private static final List<String> EXCLUDED_ROLES_FOR_ATTENDANCE = List.of("에이전시 관리자", "관리자");
    /** 건강 인원 분포에서 제외할 역할 (에이전시 관리자만 제외) */
    private static final String EXCLUDED_ROLE_FOR_HEALTH = "에이전시 관리자";

    @Override
    public AttendanceDistributionResponse getAttendanceDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        List<Member> allMembers = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<Member> members = allMembers.stream()
                .filter(m -> m.getMemberRole() == null || !EXCLUDED_ROLES_FOR_ATTENDANCE.contains(m.getMemberRole().trim()))
                .collect(Collectors.toList());
        int totalMembers = members.size();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        // 1) 오늘 포함된 승인된 근태 신청 → 휴가/재택근무/워케이션
        List<AttendanceRequest> approvedToday;
        try {
            approvedToday = attendanceRequestRepository.findApprovedByAgencyNoAndDateBetween(agencyNo, todayStart, todayEnd);
        } catch (Exception e) {
            log.warn("금일 승인 근태 신청 조회 중 오류: {}", e.getMessage());
            approvedToday = Collections.emptyList();
        }
        Map<Long, String> memberStatusFromRequest = new LinkedHashMap<>();
        for (AttendanceRequest ar : approvedToday) {
            String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
            if (displayType == null) continue;
            Long memberNo = ar.getMember().getMemberNo();
            if ("휴가".equals(displayType)) {
                memberStatusFromRequest.put(memberNo, displayType);
            }
        }
        for (AttendanceRequest ar : approvedToday) {
            String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
            if ("재택근무".equals(displayType)) {
                Long memberNo = ar.getMember().getMemberNo();
                if (!memberStatusFromRequest.containsKey(memberNo)) {
                    memberStatusFromRequest.put(memberNo, displayType);
                }
            }
        }
        for (AttendanceRequest ar : approvedToday) {
            String displayType = requestTypeToDisplay(ar.getAttendanceRequestType());
            if ("워케이션".equals(displayType)) {
                Long memberNo = ar.getMember().getMemberNo();
                if (!memberStatusFromRequest.containsKey(memberNo)) {
                    memberStatusFromRequest.put(memberNo, displayType);
                }
            }
        }

        // 2) 금일 출근/퇴근 기록(Attendance) 있는 회원 → 출근
        List<Attendance> todayRecords;
        try {
            todayRecords = attendanceRepository.findByAgency_AgencyNoAndDate(agencyNo, today);
        } catch (Exception e) {
            log.warn("금일 출석 조회 중 오류: {}", e.getMessage());
            todayRecords = Collections.emptyList();
        }
        Map<Long, Boolean> hasAttendanceToday = new LinkedHashMap<>();
        for (Attendance a : todayRecords) {
            hasAttendanceToday.putIfAbsent(a.getMember().getMemberNo(), Boolean.TRUE);
        }

        Set<Long> employeeMemberNos = members.stream().map(Member::getMemberNo).collect(Collectors.toSet());
        int countLeave = (int) memberStatusFromRequest.entrySet().stream()
                .filter(e -> "휴가".equals(e.getValue()) && employeeMemberNos.contains(e.getKey()))
                .count();
        int countRemote = (int) memberStatusFromRequest.entrySet().stream()
                .filter(e -> "재택근무".equals(e.getValue()) && employeeMemberNos.contains(e.getKey()))
                .count();
        int countWorkation = (int) memberStatusFromRequest.entrySet().stream()
                .filter(e -> "워케이션".equals(e.getValue()) && employeeMemberNos.contains(e.getKey()))
                .count();
        int countOffice = 0;
        for (Long memberNo : hasAttendanceToday.keySet()) {
            if (employeeMemberNos.contains(memberNo) && !memberStatusFromRequest.containsKey(memberNo)) {
                countOffice++;
            }
        }
        // 미출석 = 직원 중 휴가·재택·워케이션·출근이 아닌 인원만 (에이전시 관리자 제외, 휴가 중인 인원은 미출석에서 제외)
        int absent = 0;
        for (Member m : members) {
            Long memberNo = m.getMemberNo();
            if (memberStatusFromRequest.containsKey(memberNo) || hasAttendanceToday.containsKey(memberNo)) {
                continue; // 휴가/재택/워케이션/출근 중 하나에 해당 → 미출석 아님
            }
            absent++;
        }

        Map<String, String> displayColors = new LinkedHashMap<>();
        displayColors.put("출근", "#00ACC1");
        displayColors.put("재택근무", "#FF9800");
        displayColors.put("휴가", "#757575");
        displayColors.put("워케이션", "#9C27B0");
        displayColors.put("미출석", "#EF4444");

        List<AttendanceDistributionResponse.AttendanceItem> list = new ArrayList<>();
        for (String displayName : List.of("출근", "재택근무", "휴가", "워케이션", "미출석")) {
            int value;
            switch (displayName) {
                case "출근": value = countOffice; break;
                case "재택근무": value = countRemote; break;
                case "휴가": value = countLeave; break;
                case "워케이션": value = countWorkation; break;
                case "미출석": value = absent; break;
                default: value = 0;
            }
            String color = displayColors.getOrDefault(displayName, "#94a3b8");
            list.add(AttendanceDistributionResponse.AttendanceItem.builder()
                    .name(displayName)
                    .value((long) value)
                    .color(color)
                    .build());
        }
        return AttendanceDistributionResponse.builder().distribution(list).build();
    }

    @Override
    public HealthDistributionResponse getHealthDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);

        // 집계 대상: 에이전시 소속 회원 중 에이전시 관리자 제외
        List<Member> allMembers = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<Member> targetMembers = allMembers.stream()
                .filter(m -> m.getMemberRole() == null || !EXCLUDED_ROLE_FOR_HEALTH.equals(m.getMemberRole().trim()))
                .collect(Collectors.toList());
        Set<Long> targetMemberNos = targetMembers.stream().map(Member::getMemberNo).collect(Collectors.toSet());
        int totalTarget = targetMemberNos.size();

        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);

        // 설문 타입별로 필터 후 회원별 최신 제출만 사용하여 등급 집계
        List<HealthDistributionResponse.HealthItem> mentalDistribution = buildHealthDistributionForType(
                items, "월간 정신", targetMemberNos, totalTarget);
        List<HealthDistributionResponse.HealthItem> physicalDistribution = buildHealthDistributionForType(
                items, "월간 신체", targetMemberNos, totalTarget);

        return HealthDistributionResponse.builder()
                .mentalDistribution(mentalDistribution)
                .physicalDistribution(physicalDistribution)
                .build();
    }

    /**
     * 특정 설문 타입(월간 정신 / 월간 신체)에 대해 위험·경고·주의·정상·미검진 집계 (에이전시 관리자 제외 대상 기준)
     */
    private List<HealthDistributionResponse.HealthItem> buildHealthDistributionForType(
            List<HealthSurveyResponseItem> items,
            String surveyType,
            Set<Long> targetMemberNos,
            int totalTarget) {

        List<HealthSurveyResponseItem> typeItems = items.stream()
                .filter(item -> item.getHealthSurveyQuestionItemCreatedAt() != null)
                .filter(item -> surveyType.equals(item.getHealthSurveyQuestion().getHealthSurveyQuestionType()))
                .filter(item -> targetMemberNos.contains(item.getMember().getMemberNo()))
                .collect(Collectors.toList());

        Map<Long, Map<LocalDateTime, List<HealthSurveyResponseItem>>> byMemberThenByTime = typeItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMember().getMemberNo(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(HealthSurveyResponseItem::getHealthSurveyQuestionItemCreatedAt)));

        long dangerCount = 0;
        long warningCount = 0;
        long cautionCount = 0;
        long normalCount = 0;

        for (Map<LocalDateTime, List<HealthSurveyResponseItem>> byTime : byMemberThenByTime.values()) {
            LocalDateTime latestCreatedAt = byTime.keySet().stream().max(LocalDateTime::compareTo).orElse(null);
            if (latestCreatedAt == null) continue;
            List<HealthSurveyResponseItem> latestSubmission = byTime.get(latestCreatedAt);
            if (latestSubmission == null || latestSubmission.isEmpty()) continue;

            int totalScore = latestSubmission.stream()
                    .mapToInt(item -> item.getHealthSurveyQuestionItemAnswerScore() != null ? item.getHealthSurveyQuestionItemAnswerScore() : 0)
                    .sum();
            String level;
            try {
                level = healthSurveyService.evaluateRiskLevel(surveyType, totalScore);
            } catch (Exception e) {
                level = HealthSurveyRiskLevelDto.NORMAL;
            }
            if (HealthSurveyRiskLevelDto.DANGER.equals(level)) {
                dangerCount++;
            } else if (HealthSurveyRiskLevelDto.WARNING.equals(level)) {
                warningCount++;
            } else if (HealthSurveyRiskLevelDto.CAUTION.equals(level)) {
                cautionCount++;
            } else {
                normalCount++;
            }
        }

        long screenedCount = dangerCount + warningCount + cautionCount + normalCount;
        long unscreenedCount = Math.max(0, totalTarget - screenedCount);

        List<HealthDistributionResponse.HealthItem> list = new ArrayList<>();
        list.add(HealthDistributionResponse.HealthItem.builder().name("위험").value(dangerCount).color("#EF4444").build());
        list.add(HealthDistributionResponse.HealthItem.builder().name("경고").value(warningCount).color("#CA8A04").build());
        list.add(HealthDistributionResponse.HealthItem.builder().name("주의").value(cautionCount).color("#FF9800").build());
        list.add(HealthDistributionResponse.HealthItem.builder().name("정상").value(normalCount).color("#10B981").build());
        list.add(HealthDistributionResponse.HealthItem.builder().name("미검진").value(unscreenedCount).color("#94A3B8").build());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public HealthMonitoringDetailResponse getHealthMonitoringDetail(Long agencyNo, String type) {
        findAgencyOrThrow(agencyNo);
        String surveyType = "mental".equalsIgnoreCase(type) ? "월간 정신" : "월간 신체";
        String responseType = "mental".equalsIgnoreCase(type) ? "mental" : "physical";

        List<Member> allMembers = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<Member> targetMembers = allMembers.stream()
                .filter(m -> m.getMemberRole() == null || !EXCLUDED_ROLE_FOR_HEALTH.equals(m.getMemberRole().trim()))
                .collect(Collectors.toList());
        Set<Long> targetMemberNos = targetMembers.stream().map(Member::getMemberNo).collect(Collectors.toSet());

        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);
        List<HealthSurveyResponseItem> typeItems = items.stream()
                .filter(item -> item.getHealthSurveyQuestionItemCreatedAt() != null)
                .filter(item -> surveyType.equals(item.getHealthSurveyQuestion().getHealthSurveyQuestionType()))
                .filter(item -> targetMemberNos.contains(item.getMember().getMemberNo()))
                .collect(Collectors.toList());

        Map<Long, Map<LocalDateTime, List<HealthSurveyResponseItem>>> byMemberThenByTime = typeItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMember().getMemberNo(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(HealthSurveyResponseItem::getHealthSurveyQuestionItemCreatedAt)));

        List<HealthMonitoringDetailResponse.HealthMonitoringDetailItem> result = new ArrayList<>();
        Set<Long> screenedMemberNos = new HashSet<>();

        for (Map.Entry<Long, Map<LocalDateTime, List<HealthSurveyResponseItem>>> entry : byMemberThenByTime.entrySet()) {
            Long memberNo = entry.getKey();
            Map<LocalDateTime, List<HealthSurveyResponseItem>> byTime = entry.getValue();
            LocalDateTime latestCreatedAt = byTime.keySet().stream().max(LocalDateTime::compareTo).orElse(null);
            if (latestCreatedAt == null) continue;
            List<HealthSurveyResponseItem> latestSubmission = byTime.get(latestCreatedAt);
            if (latestSubmission == null || latestSubmission.isEmpty()) continue;

            Member member = latestSubmission.get(0).getMember();
            int totalScore = latestSubmission.stream()
                    .mapToInt(item -> item.getHealthSurveyQuestionItemAnswerScore() != null ? item.getHealthSurveyQuestionItemAnswerScore() : 0)
                    .sum();
            String level;
            try {
                level = healthSurveyService.evaluateRiskLevel(surveyType, totalScore);
            } catch (Exception e) {
                level = HealthSurveyRiskLevelDto.NORMAL;
            }
            screenedMemberNos.add(memberNo);
            String lastCheckDate = latestCreatedAt.toLocalDate().toString();
            String position = member.getMemberRole() != null ? member.getMemberRole() : "";
            result.add(HealthMonitoringDetailResponse.HealthMonitoringDetailItem.builder()
                    .memberNo(memberNo)
                    .memberName(member.getMemberName() != null ? member.getMemberName() : "")
                    .position(position)
                    .totalScore(totalScore)
                    .status(level)
                    .lastCheckDate(lastCheckDate)
                    .build());
        }

        for (Member m : targetMembers) {
            if (screenedMemberNos.contains(m.getMemberNo())) continue;
            result.add(HealthMonitoringDetailResponse.HealthMonitoringDetailItem.builder()
                    .memberNo(m.getMemberNo())
                    .memberName(m.getMemberName() != null ? m.getMemberName() : "")
                    .position(m.getMemberRole() != null ? m.getMemberRole() : "")
                    .totalScore(null)
                    .status("미검진")
                    .lastCheckDate(null)
                    .build());
        }

        result.sort((a, b) -> {
            boolean aUnscreened = "미검진".equals(a.getStatus());
            boolean bUnscreened = "미검진".equals(b.getStatus());
            if (aUnscreened && !bUnscreened) return 1;
            if (!aUnscreened && bUnscreened) return -1;
            if (aUnscreened && bUnscreened) return (a.getMemberName()).compareTo(b.getMemberName());
            int orderA = riskOrder(a.getStatus());
            int orderB = riskOrder(b.getStatus());
            if (orderA != orderB) return Integer.compare(orderA, orderB);
            String dateA = a.getLastCheckDate() != null ? a.getLastCheckDate() : "";
            String dateB = b.getLastCheckDate() != null ? b.getLastCheckDate() : "";
            int dateCmp = dateB.compareTo(dateA);
            if (dateCmp != 0) return dateCmp;
            Integer scoreA = a.getTotalScore() != null ? a.getTotalScore() : 0;
            Integer scoreB = b.getTotalScore() != null ? b.getTotalScore() : 0;
            return Integer.compare(scoreB, scoreA);
        });

        return HealthMonitoringDetailResponse.builder()
                .type(responseType)
                .items(result)
                .build();
    }

    private static int riskOrder(String status) {
        if ("위험".equals(status)) return 0;
        if ("경고".equals(status)) return 1;
        if ("주의".equals(status)) return 2;
        if ("정상".equals(status)) return 3;
        return 4;
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyHealthScheduleResponse getAgencyHealthSchedule(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        java.util.Optional<HealthSurvey> opt = healthSurveyRepository.findByAgency_AgencyNo(agencyNo);
        LocalDate today = LocalDate.now();
        int period = 15;
        int cycle = 30;
        LocalDate baseDate = today;
        String createdAtStr = null;

        if (opt.isPresent()) {
            HealthSurvey hs = opt.get();
            period = hs.getHealthSurveyPeriod() != null ? hs.getHealthSurveyPeriod() : 15;
            cycle = hs.getHealthSurveyCycle() != null ? hs.getHealthSurveyCycle() : 30;
            if (hs.getHealthSurveyCreatedAt() != null) {
                baseDate = hs.getHealthSurveyCreatedAt().toLocalDate();
                createdAtStr = baseDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            }
        }

        LocalDate next = baseDate;
        while (!next.isAfter(today)) {
            next = next.plusDays(cycle);
        }
        long daysUntil = ChronoUnit.DAYS.between(today, next);
        String nextCheckupDateStr = next.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        return AgencyHealthScheduleResponse.builder()
                .nextCheckupDate(nextCheckupDateStr)
                .daysUntil((int) daysUntil)
                .period(period)
                .cycle(cycle)
                .createdAt(createdAtStr)
                .build();
    }

    @Override
    @Transactional
    public void updateAgencyHealthSchedule(Long agencyNo, UpdateHealthScheduleRequest request) {
        Agency agency = findAgencyOrThrow(agencyNo);
        int period = request.getPeriod() != null && request.getPeriod() >= 1 && request.getPeriod() <= 365
                ? request.getPeriod() : 15;
        int cycle = request.getCycle() != null && request.getCycle() >= 1 && request.getCycle() <= 365
                ? request.getCycle() : 30;

        java.util.Optional<HealthSurvey> opt = healthSurveyRepository.findByAgency_AgencyNo(agencyNo);
        HealthSurvey hs;
        if (opt.isPresent()) {
            hs = opt.get();
            hs.setHealthSurveyPeriod(period);
            hs.setHealthSurveyCycle(cycle);
        } else {
            hs = HealthSurvey.builder()
                    .agency(agency)
                    .healthSurveyPeriod(period)
                    .healthSurveyCycle(cycle)
                    .build();
        }
        healthSurveyRepository.save(hs);
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyUnscreenedListResponse getAgencyUnscreenedList(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        LocalDate today = LocalDate.now();

        // 검진 만료일 = HEALTH_SURVEY_PERIOD 계산 (현재 구간 cycleStart + period - 1)
        // 지연 일수 = CREATED_AT + CYCLE 기준 검진 시작일(cycleStart)부터 오늘까지 검사 안 한 일수
        java.util.Optional<HealthSurvey> healthSurveyOpt = healthSurveyRepository.findByAgency_AgencyNo(agencyNo);
        int period = 15;
        int cycle = 30;
        LocalDate baseDate = today;
        if (healthSurveyOpt.isPresent()) {
            HealthSurvey hs = healthSurveyOpt.get();
            period = hs.getHealthSurveyPeriod() != null ? hs.getHealthSurveyPeriod() : 15;
            cycle = hs.getHealthSurveyCycle() != null ? hs.getHealthSurveyCycle() : 30;
            if (hs.getHealthSurveyCreatedAt() != null) {
                baseDate = hs.getHealthSurveyCreatedAt().toLocalDate();
            }
        }
        long daysSinceBase = ChronoUnit.DAYS.between(baseDate, today);
        int k = cycle > 0 ? (int) (daysSinceBase / cycle) : 0;
        if (daysSinceBase < 0) k = 0;
        LocalDate cycleStart = baseDate.plusDays((long) k * cycle);
        LocalDate deadline = cycleStart.plusDays(Math.max(0, period - 1));
        String nextCheckupDateStr = deadline.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        int daysOverdue = !today.isBefore(cycleStart) ? (int) ChronoUnit.DAYS.between(cycleStart, today) : 0;

        List<Member> allMembers = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<Member> targetMembers = allMembers.stream()
                .filter(m -> m.getMemberRole() == null || !EXCLUDED_ROLE_FOR_HEALTH.equals(m.getMemberRole().trim()))
                .collect(Collectors.toList());

        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);
        Map<Long, LocalDateTime> mentalLatestByMember = getLatestSubmissionByMember(items, "월간 정신");
        Map<Long, LocalDateTime> physicalLatestByMember = getLatestSubmissionByMember(items, "월간 신체");

        List<AgencyUnscreenedListResponse.UnscreenedItem> result = new ArrayList<>();
        for (Member m : targetMembers) {
            Long memberNo = m.getMemberNo();
            LocalDateTime mentalLatest = mentalLatestByMember.get(memberNo);
            LocalDateTime physicalLatest = physicalLatestByMember.get(memberNo);
            boolean mentalScreened = mentalLatest != null;
            boolean physicalScreened = physicalLatest != null;
            if (mentalScreened && physicalScreened) continue;

            String status;
            if (!mentalScreened && !physicalScreened) {
                status = "BOTH";
            } else if (!mentalScreened) {
                status = "MENTAL_ONLY";
            } else {
                status = "PHYSICAL_ONLY";
            }
            String lastMental = mentalLatest != null ? mentalLatest.toLocalDate().toString() : null;
            String lastPhysical = physicalLatest != null ? physicalLatest.toLocalDate().toString() : null;
            result.add(AgencyUnscreenedListResponse.UnscreenedItem.builder()
                    .memberNo(memberNo)
                    .memberName(m.getMemberName() != null ? m.getMemberName() : "")
                    .position(m.getMemberRole() != null ? m.getMemberRole() : "")
                    .status(status)
                    .lastMentalCheckDate(lastMental)
                    .lastPhysicalCheckDate(lastPhysical)
                    .daysOverdue(daysOverdue)
                    .build());
        }

        return AgencyUnscreenedListResponse.builder()
                .nextCheckupDate(nextCheckupDateStr)
                .items(result)
                .build();
    }

    private static final String[] DEADLINE_DAY_LABELS = {"오늘", "내일", "2일 후", "3일 후", "4일 후"};

    @Override
    @Transactional(readOnly = true)
    public List<AgencyDeadlineCountResponse.DeadlineItem> getAgencyDeadlineCounts(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        List<Long> projectNos = projectMemberRepository.findDistinctProjectNosByAgencyNoAndManagerRole(agencyNo);
        LocalDate today = LocalDate.now();
        int[] counts = new int[5];

        for (Long projectNo : projectNos) {
            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(projectNo);
            for (KanbanCard card : cards) {
                if ("D".equals(card.getKanbanCardStatus())) continue;
                LocalDate endedAt = card.getKanbanCardEndedAt();
                if (endedAt == null) continue;
                long daysDiff = ChronoUnit.DAYS.between(today, endedAt);
                if (daysDiff >= 0 && daysDiff <= 4) {
                    counts[(int) daysDiff]++;
                }
            }
        }

        List<AgencyDeadlineCountResponse.DeadlineItem> result = new ArrayList<>();
        for (int i = 0; i < DEADLINE_DAY_LABELS.length; i++) {
            result.add(AgencyDeadlineCountResponse.DeadlineItem.builder()
                    .name(DEADLINE_DAY_LABELS[i])
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    /** 설문 타입별 회원의 최신 제출 시각 맵 */
    private Map<Long, LocalDateTime> getLatestSubmissionByMember(
            List<HealthSurveyResponseItem> items,
            String surveyType) {
        List<HealthSurveyResponseItem> typeItems = items.stream()
                .filter(item -> item.getHealthSurveyQuestionItemCreatedAt() != null)
                .filter(item -> surveyType.equals(item.getHealthSurveyQuestion().getHealthSurveyQuestionType()))
                .collect(Collectors.toList());
        Map<Long, Map<LocalDateTime, List<HealthSurveyResponseItem>>> byMemberThenByTime = typeItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMember().getMemberNo(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(HealthSurveyResponseItem::getHealthSurveyQuestionItemCreatedAt)));
        Map<Long, LocalDateTime> latestByMember = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<LocalDateTime, List<HealthSurveyResponseItem>>> e : byMemberThenByTime.entrySet()) {
            LocalDateTime latest = e.getValue().keySet().stream().max(LocalDateTime::compareTo).orElse(null);
            if (latest != null) {
                latestByMember.put(e.getKey(), latest);
            }
        }
        return latestByMember;
    }

    private static final String HEALTH_REMINDER_TYPE = "HEALTH_REM";
    private static final String HEALTH_REMINDER_NAME = "검진 알림";
    private static final String HEALTH_REMINDER_TEXT = "건강 검진을 완료해 주세요.";

    @Override
    @Transactional
    public void sendUnscreenedNotification(Long agencyNo, Long memberNo) {
        findAgencyOrThrow(agencyNo);
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (member.getAgency() == null || !agencyNo.equals(member.getAgency().getAgencyNo())) {
            throw new IllegalArgumentException("해당 에이전시 소속 회원이 아닙니다.");
        }
        notificationService.createNotification(memberNo, HEALTH_REMINDER_NAME, HEALTH_REMINDER_TEXT, HEALTH_REMINDER_TYPE);
    }

    @Override
    @Transactional
    public void sendUnscreenedBulkNotification(Long agencyNo) {
        AgencyUnscreenedListResponse list = getAgencyUnscreenedList(agencyNo);
        List<AgencyUnscreenedListResponse.UnscreenedItem> overdue = list.getItems().stream()
                .filter(item -> item.getDaysOverdue() != null && item.getDaysOverdue() >= 7)
                .collect(Collectors.toList());
        for (AgencyUnscreenedListResponse.UnscreenedItem item : overdue) {
            try {
                notificationService.createNotification(
                        item.getMemberNo(), HEALTH_REMINDER_NAME, HEALTH_REMINDER_TEXT, HEALTH_REMINDER_TYPE);
            } catch (Exception e) {
                log.warn("미검진 알림 발송 실패 memberNo={}", item.getMemberNo(), e);
            }
        }
    }
}
