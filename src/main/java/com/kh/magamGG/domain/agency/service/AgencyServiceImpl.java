package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.*;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.mapper.AgencyMapper;
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
import java.util.ArrayList;
import java.util.Collections;
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
    private final AgencyMapper agencyMapper; // MyBatis Mapper
    private final NotificationService notificationService;
    private final ManagerRepository managerRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;

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

        // 1. NEW_REQUEST 상태를 "승인"으로 변경 (MyBatis 직접 SQL)
        int requestUpdated = agencyMapper.updateNewRequestStatus(newRequestNo, "승인");
        if (requestUpdated == 0) {
            throw new IllegalStateException("가입 요청 상태 업데이트에 실패했습니다.");
        }
        log.info("NEW_REQUEST 상태 업데이트 완료: {} -> 승인", newRequestNo);

        // 2. MEMBER의 AGENCY_NO를 업데이트 (MyBatis 직접 SQL)
        int memberUpdated = agencyMapper.updateMemberAgencyNo(memberNo, agencyNo);
        if (memberUpdated == 0) {
            throw new IllegalStateException("회원의 에이전시 정보 업데이트에 실패했습니다.");
        }
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

        // 가입 요청 상태를 "거절"로 변경 (MyBatis 직접 SQL)
        int updated = agencyMapper.updateNewRequestStatus(newRequestNo, "거절");
        if (updated == 0) {
            throw new IllegalStateException("가입 요청 상태 업데이트에 실패했습니다.");
        }
        
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
    public DashboardMetricsResponse getDashboardMetrics(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        int artistCount = 0;
        int projectCount = 0;
        try {
            artistCount = memberRepository.findArtistsByAgencyNo(agencyNo).size();
            projectCount = (int) projectRepository.count();
        } catch (Exception e) {
            log.warn("대시보드 메트릭 조회 중 일부 데이터 누락: {}", e.getMessage());
        }
        return DashboardMetricsResponse.builder()
                .averageDeadlineComplianceRate(0)
                .activeArtistCount(artistCount)
                .activeProjectCount(projectCount)
                .complianceRateChange("-")
                .activeArtistChange("-")
                .activeProjectChange("-")
                .build();
    }

    @Override
    public ComplianceTrendResponse getComplianceTrend(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        return ComplianceTrendResponse.builder()
                .trend(Collections.emptyList())
                .monthOverMonthChange(null)
                .build();
    }

    @Override
    public ArtistDistributionResponse getArtistDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        if (artists.isEmpty()) {
            return ArtistDistributionResponse.builder()
                    .distribution(Collections.emptyList())
                    .maxArtistProjectName(null)
                    .build();
        }
        List<Long> memberNos = artists.stream().map(Member::getMemberNo).collect(Collectors.toList());
        List<Object[]> rows;
        try {
            rows = projectMemberRepository.countArtistsByProjectForMembers(memberNos);
        } catch (Exception e) {
            log.warn("작품별 아티스트 분포 조회 중 오류: {}", e.getMessage());
            return ArtistDistributionResponse.builder()
                    .distribution(Collections.emptyList())
                    .maxArtistProjectName(null)
                    .build();
        }
        List<ArtistDistributionResponse.DistributionItem> distribution = new ArrayList<>();
        String maxArtistProjectName = null;
        int maxArtists = 0;
        for (Object[] row : rows) {
            String projectName = row[0] != null ? String.valueOf(row[0]) : "-";
            Number count = row[1] instanceof Number ? (Number) row[1] : null;
            int artistsCount = count != null ? count.intValue() : 0;
            distribution.add(ArtistDistributionResponse.DistributionItem.builder()
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

        List<AttendanceDistributionResponse.DistributionItem> list = new ArrayList<>();
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
            list.add(AttendanceDistributionResponse.DistributionItem.builder()
                    .name(displayName)
                    .value(value)
                    .color(color)
                    .build());
        }
        return AttendanceDistributionResponse.builder().distribution(list).build();
    }

    @Override
    public HealthDistributionResponse getHealthDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        List<HealthDistributionResponse.DistributionItem> list = new ArrayList<>();
        list.add(HealthDistributionResponse.DistributionItem.builder().name("위험").value(0).color("#EF4444").build());
        list.add(HealthDistributionResponse.DistributionItem.builder().name("주의").value(0).color("#FF9800").build());
        list.add(HealthDistributionResponse.DistributionItem.builder().name("정상").value(0).color("#10B981").build());
        return HealthDistributionResponse.builder().distribution(list).build();
    }
}
