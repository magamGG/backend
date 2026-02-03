package com.kh.magamGG.domain.agency.service;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.ArtistDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.AttendanceDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.mapper.AgencyMapper;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
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
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.domain.health.dto.HealthSurveyRiskLevelDto;
import com.kh.magamGG.domain.health.entity.HealthSurveyResponseItem;
import com.kh.magamGG.domain.health.repository.HealthSurveyResponseItemRepository;
import com.kh.magamGG.domain.health.service.HealthSurveyService;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import com.kh.magamGG.global.exception.NewRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final AgencyMapper agencyMapper; // MyBatis Mapper
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
        // 마감일 지난 카드 중 완료(Y)된 비율
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
        Double currRate = null;

        for (Map.Entry<YearMonth, long[]> e : monthStats.entrySet()) {
            long total = e.getValue()[0];
            long completed = e.getValue()[1];
            double rate = total > 0 ? Math.round((completed * 100.0 / total) * 10) / 10.0 : 100.0;
            String monthLabel = e.getKey().getMonthValue() + "월";
            trend.add(new ComplianceTrendResponse.ComplianceMonthItem(monthLabel, rate));

            if (prevRate != null) {
                currRate = rate;
                monthOverMonthChange = Math.round((currRate - prevRate) * 10) / 10.0;
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

        List<com.kh.magamGG.domain.project.entity.Project> projects =
                projectRepository.findAllProjectsByAgencyNo(agencyNo);

        List<ArtistDistributionResponse.ArtistDistributionItem> distribution = new ArrayList<>();
        String maxProjectName = null;
        long maxArtists = 0;

        for (var project : projects) {
            long artistCount = projectMemberRepository.countArtistsByProjectAndAgency(
                    project.getProjectNo(), agencyNo);

            distribution.add(new ArtistDistributionResponse.ArtistDistributionItem(
                    project.getProjectName(), artistCount));

            if (artistCount > maxArtists) {
                maxArtists = artistCount;
                maxProjectName = project.getProjectName();
            }
        }

        distribution.sort((a, b) -> Long.compare(b.getArtists(), a.getArtists()));

        return ArtistDistributionResponse.builder()
                .distribution(distribution)
                .maxArtistProjectName(maxProjectName)
                .build();
    }

    @Override
    public AttendanceDistributionResponse getAttendanceDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);
        LocalDate today = LocalDate.now();

        // 에이전시 소속 작가(웹툰/웹소설) 목록
        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        Set<Long> artistNos = artists.stream().map(Member::getMemberNo).collect(Collectors.toSet());

        // 출근: 오늘 체크인한 회원
        List<Long> checkedInNos = attendanceRepository.findMemberNosCheckedInByAgencyAndDate(agencyNo, today);
        Set<Long> checkedInSet = new HashSet<>(checkedInNos);

        // 승인된 근태 신청 중 오늘 포함되는 것
        List<AttendanceRequest> approvedRequests = attendanceRequestRepository.findByAgencyNoWithMember(agencyNo)
                .stream()
                .filter(r -> "APPROVED".equals(r.getAttendanceRequestStatus()))
                .filter(r -> {
                    LocalDate start = r.getAttendanceRequestStartDate().toLocalDate();
                    LocalDate end = r.getAttendanceRequestEndDate().toLocalDate();
                    return !today.isBefore(start) && !today.isAfter(end);
                })
                .collect(Collectors.toList());

        Set<Long> inWorkcation = new HashSet<>();
        Set<Long> inRemote = new HashSet<>();
        Set<Long> inLeave = new HashSet<>();

        for (AttendanceRequest req : approvedRequests) {
            Long memberNo = req.getMember().getMemberNo();
            if (!artistNos.contains(memberNo)) continue;

            String type = req.getAttendanceRequestType();
            if ("워케이션".equals(type)) {
                inWorkcation.add(memberNo);
            } else if ("재택근무".equals(type)) {
                inRemote.add(memberNo);
            } else if ("휴재".equals(type) || "휴가".equals(type) || "반차".equals(type) || "병가".equals(type)) {
                inLeave.add(memberNo);
            }
        }

        // 우선순위: 워케이션 > 재택근무 > 휴재 > 출근 (한 사람은 하나의 카테고리만)
        Set<Long> assigned = new HashSet<>();
        assigned.addAll(inWorkcation);
        assigned.addAll(inRemote);
        assigned.addAll(inLeave);

        long workcationCount = inWorkcation.size();
        long remoteCount = inRemote.size();
        long leaveCount = inLeave.size();
        long checkedInCount = checkedInSet.stream().filter(n -> !assigned.contains(n) && artistNos.contains(n)).count();

        List<AttendanceDistributionResponse.AttendanceItem> distribution = new ArrayList<>();
        distribution.add(new AttendanceDistributionResponse.AttendanceItem("출근", checkedInCount, "#00ACC1"));
        distribution.add(new AttendanceDistributionResponse.AttendanceItem("재택근무", remoteCount, "#FF9800"));
        distribution.add(new AttendanceDistributionResponse.AttendanceItem("휴재", leaveCount, "#757575"));
        distribution.add(new AttendanceDistributionResponse.AttendanceItem("워케이션", workcationCount, "#9C27B0"));

        return AttendanceDistributionResponse.builder()
                .distribution(distribution)
                .build();
    }

    @Override
    public HealthDistributionResponse getHealthDistribution(Long agencyNo) {
        findAgencyOrThrow(agencyNo);

        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);
        Map<Long, HealthSurveyResponseItem> latestByMember = new LinkedHashMap<>();
        for (HealthSurveyResponseItem item : items) {
            Long memberNo = item.getMember().getMemberNo();
            if (!latestByMember.containsKey(memberNo)) {
                latestByMember.put(memberNo, item);
            }
        }

        long dangerCount = 0;
        long cautionCount = 0;
        long normalCount = 0;

        for (HealthSurveyResponseItem item : latestByMember.values()) {
            String surveyType = item.getHealthSurveyQuestion().getHealthSurvey().getHealthSurveyType();
            int score = item.getHealthSurveyQuestionItemAnswerScore();
            String level;
            try {
                level = healthSurveyService.evaluateRiskLevel(surveyType, score);
            } catch (Exception e) {
                level = HealthSurveyRiskLevelDto.NORMAL;
            }
            if (HealthSurveyRiskLevelDto.DANGER.equals(level)) {
                dangerCount++;
            } else if (HealthSurveyRiskLevelDto.WARNING.equals(level) || HealthSurveyRiskLevelDto.CAUTION.equals(level)) {
                cautionCount++;
            } else {
                normalCount++;
            }
        }

        List<HealthDistributionResponse.HealthItem> distribution = new ArrayList<>();
        distribution.add(new HealthDistributionResponse.HealthItem("위험", dangerCount, "#EF4444"));
        distribution.add(new HealthDistributionResponse.HealthItem("주의", cautionCount, "#FF9800"));
        distribution.add(new HealthDistributionResponse.HealthItem("정상", normalCount, "#10B981"));

        return HealthDistributionResponse.builder()
                .distribution(distribution)
                .build();
    }
}
