package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.agency.dto.response.AgencyDeadlineCountResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.dto.response.ComplianceTrendResponse;
import com.kh.magamGG.domain.agency.dto.response.HealthDistributionResponse;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveBalanceResponse;
import com.kh.magamGG.domain.attendance.service.AttendanceService;
import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.dto.QuickReportResponse;
import com.kh.magamGG.domain.health.dto.response.HealthSurveyResponseStatusResponse;
import com.kh.magamGG.domain.health.service.HealthSurveyService;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.dto.response.WorkingArtistResponse;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.service.MemberService;
import com.kh.magamGG.domain.project.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.project.dto.response.DelayedTaskItemResponse;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.dto.response.TodayTaskResponse;
import com.kh.magamGG.domain.project.service.KanbanBoardService;
import com.kh.magamGG.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String PENDING = "PENDING";

    private final KanbanBoardService kanbanBoardService;
    private final AttendanceService attendanceService;
    private final ProjectService projectService;
    private final AgencyService agencyService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final HealthSurveyService healthSurveyService;
    private final ManagerRepository managerRepository;

    @Override
    public ChatResponse processChat(ChatRequest request, Long memberNo) {
        return ChatResponse.builder()
                .message("아래 분석 버튼이나 서비스 안내를 이용해 주세요.")
                .action(null)
                .build();
    }

    @Override
    public boolean isAIAvailable() {
        return false;
    }

    private QuickReportResponse report(String message) {
        return QuickReportResponse.builder().message(message).build();
    }

    private QuickReportResponse report(String message, List<QuickReportResponse.ActionItem> actions) {
        return QuickReportResponse.builder().message(message).actions(actions).build();
    }

    @Override
    public QuickReportResponse getQuickReport(String type, Long memberNo) {
        if (type == null || type.isBlank() || memberNo == null) {
            return report("조회할 항목을 선택해 주세요.");
        }
        String t = type.trim().toLowerCase();
        try {
            // ----- 준수율 하위 TOP 3 (관리자/담당자) -----
            if ("compliance_top3".equals(t)) {
                List<ManagedProjectResponse> projects = projectService.getManagedProjectsByManager(memberNo);
                List<ManagedProjectResponse> list = projects != null ? projects : List.of();
                if (list.isEmpty()) return report("담당 프로젝트가 없습니다.");
                List<ManagedProjectResponse> sorted = list.stream()
                        .sorted(Comparator.comparingInt(p -> p.getProgress() != null ? p.getProgress() : 100))
                        .limit(3)
                        .toList();
                StringBuilder sb = new StringBuilder();
                sb.append("📉 현재 위험 프로젝트 ").append(sorted.size()).append("곳을 찾았습니다.\n");
                sb.append("상세 현황을 확인해보세요.\n\n");
                for (int i = 0; i < sorted.size(); i++) {
                    ManagedProjectResponse p = sorted.get(i);
                    int progress = p.getProgress() != null ? p.getProgress() : 0;
                    String icon = progress < 70 ? "🔴" : "🟡";
                    sb.append((i + 1)).append(". ").append(p.getProjectName() != null ? p.getProjectName() : "(이름 없음)")
                            .append(" (").append(icon).append(" ").append(progress).append("%)\n");
                    int delayCount = projectService.getDelayCountForProject(p.getProjectNo());
                    if (delayCount > 0) sb.append("└ 최근 지연 ").append(delayCount).append("건 발생\n");
                }
                sb.append("\n👇 상세 보기");
                List<QuickReportResponse.ActionItem> actions = List.of(
                        QuickReportResponse.ActionItem.builder().label("📂 프로젝트 대시보드로 이동").sectionKeyword("대시보드").build(),
                        QuickReportResponse.ActionItem.builder().label("📊 전체 리스트 보기").sectionKeyword("프로젝트").build()
                );
                return report(sb.toString().trim(), actions);
            }

            // ----- 마감 임박/지연 (담당자) -----
            if ("deadline_urgent".equals(t)) {
                List<DelayedTaskItemResponse> delayed = projectService.getDelayedTasksForManager(memberNo);
                List<AttendanceRequestResponse> requests = attendanceService.getAttendanceRequestsByManager(memberNo);
                List<AttendanceRequestResponse> pending = requests != null
                        ? requests.stream().filter(r -> PENDING.equals(r.getAttendanceRequestStatus())).limit(5).toList()
                        : List.of();
                int totalDelayed = delayed.size();
                StringBuilder sb = new StringBuilder();
                sb.append("🔥 매니저님, 긴급 확인이 필요해요!\n");
                if (totalDelayed > 0) {
                    sb.append("현재 총 ").append(totalDelayed).append("건의 작업이 지연되고 있습니다.\n\n");
                    for (DelayedTaskItemResponse task : delayed) {
                        String emoji = task.getDaysDelayed() >= 2 ? " 😱" : "";
                        sb.append("(").append(task.getTitle()).append(") ").append(task.getArtistName())
                                .append(" - ").append(task.getDaysDelayed()).append("일 지연").append(emoji).append("\n");
                    }
                }
                if (!pending.isEmpty()) {
                    if (totalDelayed > 0) sb.append("\n");
                    for (AttendanceRequestResponse r : pending) {
                        String typeName = r.getAttendanceRequestType() != null ? r.getAttendanceRequestType() : "근태";
                        sb.append("(").append(typeName).append(") ").append(r.getMemberName() != null ? r.getMemberName() : "").append(" - 승인 대기 중\n");
                    }
                }
                if (totalDelayed == 0 && pending.isEmpty()) {
                    return report("현재 지연된 작업이나 승인 대기 건이 없습니다.");
                }
                sb.append("\n👇 바로 가기");
                List<QuickReportResponse.ActionItem> actions = List.of(
                        QuickReportResponse.ActionItem.builder().label("🏃‍♂️ 칸반 보드 바로가기").sectionKeyword("프로젝트").build()
                );
                return report(sb.toString().trim(), actions);
            }

            // ----- 아티스트(작가) -----
            if ("leave_balance".equals(t)) {
                LeaveBalanceResponse balance = attendanceService.getLeaveBalance(memberNo);
                if (balance == null) return report("연차 잔액 정보가 없습니다. 에이전시에 문의해 주세요.");
                double remain = balance.getLeaveBalanceRemainDays() != null ? balance.getLeaveBalanceRemainDays() : 0;
                int remainDays = (int) Math.round(remain);
                return report(String.format("현재 사용 가능한 연차는 %d일입니다.", remainDays));
            }
            if ("today_deadline".equals(t)) {
                List<TodayTaskResponse> tasks = kanbanBoardService.getTodayTasksForMember(memberNo);
                List<TodayTaskResponse> list = tasks != null ? tasks : List.of();
                if (list.isEmpty()) return report("오늘 마감인 작업이 없습니다.");
                StringBuilder sb = new StringBuilder();
                sb.append("오늘 마감인 작업이 ").append(list.size()).append("건 있습니다.\n");
                for (TodayTaskResponse task : list) {
                    sb.append("• ").append(task.getTitle() != null ? task.getTitle() : "(제목 없음)");
                    if (task.getProjectName() != null) sb.append(" (").append(task.getProjectName()).append(")");
                    sb.append("\n");
                }
                return report(sb.toString().trim());
            }
            if ("leave_summary".equals(t)) {
                List<AttendanceRequestResponse> myRequests = attendanceService.getAttendanceRequestsByMember(memberNo);
                if (myRequests == null) return report("이번 달 승인된 휴재·연차 내역이 없습니다.");
                YearMonth thisMonth = YearMonth.now();
                LocalDate firstDay = thisMonth.atDay(1);
                LocalDate lastDay = thisMonth.atEndOfMonth();
                List<AttendanceRequestResponse> thisMonthApproved = myRequests.stream()
                        .filter(r -> "APPROVED".equals(r.getAttendanceRequestStatus()))
                        .filter(r -> r.getAttendanceRequestStartDate() != null && r.getAttendanceRequestEndDate() != null)
                        .filter(r -> {
                            LocalDate start = r.getAttendanceRequestStartDate().toLocalDate();
                            LocalDate end = r.getAttendanceRequestEndDate().toLocalDate();
                            return !start.isAfter(lastDay) && !end.isBefore(firstDay);
                        })
                        .sorted(Comparator.comparing(AttendanceRequestResponse::getAttendanceRequestStartDate))
                        .toList();
                if (thisMonthApproved.isEmpty()) return report("이번 달 승인된 휴재·연차 내역이 없습니다.");
                StringBuilder sb = new StringBuilder();
                sb.append("이번 달 승인된 휴재·연차 내역입니다.\n");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
                for (AttendanceRequestResponse r : thisMonthApproved) {
                    String typeName = r.getAttendanceRequestType() != null ? r.getAttendanceRequestType() : "근태";
                    String start = r.getAttendanceRequestStartDate().toLocalDate().format(fmt);
                    String end = r.getAttendanceRequestEndDate().toLocalDate().format(fmt);
                    sb.append("• ").append(typeName).append(" ").append(start).append(" ~ ").append(end);
                    if (r.getAttendanceRequestUsingDays() != null) sb.append(" (").append(r.getAttendanceRequestUsingDays()).append("일)");
                    sb.append("\n");
                }
                return report(sb.toString().trim());
            }
            if ("latest_health".equals(t)) {
                HealthSurveyResponseStatusResponse mental = healthSurveyService.getSurveyResponseStatus(memberNo, "월간 정신");
                HealthSurveyResponseStatusResponse physical = healthSurveyService.getSurveyResponseStatus(memberNo, "월간 신체");
                if ((mental == null || !mental.isCompleted()) && (physical == null || !physical.isCompleted())) {
                    return report("아직 제출한 검진 결과가 없습니다. 건강관리에서 검진을 제출해 주세요.");
                }
                StringBuilder sb = new StringBuilder();
                sb.append("📊 건강관리 분석 결과").append("\n").append("\n");
                sb.append("【정신건강】").append("\n");
                if (mental != null && mental.isCompleted() && mental.getLastCheckDate() != null) {
                    int mScore = mental.getTotalScore() != null ? mental.getTotalScore() : 0;
                    String mLevel = mental.getRiskLevel() != null ? mental.getRiskLevel() : "확인 필요";
                    String mDate = mental.getLastCheckDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                    sb.append("총점: ").append(mScore).append("점 | 상태: ").append(mLevel).append("\n");
                    sb.append("최근 검진: ").append(mDate).append("\n");
                } else {
                    sb.append("미제출").append("\n");
                }
                sb.append("\n");
                sb.append("【신체건강】").append("\n");
                if (physical != null && physical.isCompleted() && physical.getLastCheckDate() != null) {
                    int pScore = physical.getTotalScore() != null ? physical.getTotalScore() : 0;
                    String pLevel = physical.getRiskLevel() != null ? physical.getRiskLevel() : "확인 필요";
                    String pDate = physical.getLastCheckDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                    sb.append("총점: ").append(pScore).append("점 | 상태: ").append(pLevel).append("\n");
                    sb.append("최근 검진: ").append(pDate).append("\n");
                } else {
                    sb.append("미제출").append("\n");
                }
                return report(sb.toString().trim());
            }

            // ----- 담당자(Manager) -----
            Optional<com.kh.magamGG.domain.member.entity.Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
            if (managerOpt.isPresent()) {
                Long managerNo = managerOpt.get().getManagerNo();
                if ("attendance_status".equals(t)) {
                    List<MemberResponse> assigned = memberService.getAssignedArtistsByMemberNo(memberNo);
                    int total = assigned != null ? assigned.size() : 0;
                    List<WorkingArtistResponse> working = null;
                    try {
                        working = memberService.getWorkingArtistsByManagerNo(managerNo);
                    } catch (Exception e) {
                        log.debug("getWorkingArtistsByManagerNo 실패: {}", e.getMessage());
                    }
                    int workingCount = working != null ? working.size() : 0;
                    return report(String.format("현재 담당 작가 %d명 중 %d명이 작업 중입니다.", total, workingCount));
                }
                if ("project_compliance".equals(t)) {
                    List<ManagedProjectResponse> projects = projectService.getManagedProjectsByManager(memberNo);
                    if (projects == null || projects.isEmpty()) return report("담당 프로젝트가 없습니다.");
                    StringBuilder sb = new StringBuilder();
                    sb.append("담당 프로젝트별 준수율입니다.\n");
                    for (ManagedProjectResponse p : projects) {
                        String name = p.getProjectName() != null ? p.getProjectName() : "(이름 없음)";
                        int progress = p.getProgress() != null ? p.getProgress() : 0;
                        sb.append("• ").append(name).append(": ").append(progress).append("%\n");
                    }
                    return report(sb.toString().trim());
                }
                if ("top3_leave_artists".equals(t)) {
                    List<AttendanceRequestResponse> all;
                    try {
                        all = attendanceService.getAttendanceRequestsByManager(memberNo);
                    } catch (Exception e) {
                        log.warn("getAttendanceRequestsByManager 실패: {}", e.getMessage());
                        return report("근태 신청 목록을 불러오는 중 오류가 발생했습니다.");
                    }
                    if (all == null) all = List.of();
                    LocalDateTime since = LocalDateTime.now().minusMonths(3);
                    Map<String, Long> countByName = all.stream()
                            .filter(r -> r != null && "APPROVED".equals(r.getAttendanceRequestStatus()))
                            .filter(r -> "휴재".equals(r.getAttendanceRequestType()))
                            .filter(r -> r.getAttendanceRequestCreatedAt() != null && !r.getAttendanceRequestCreatedAt().isBefore(since))
                            .collect(Collectors.groupingBy(r -> r.getMemberName() != null ? r.getMemberName() : "알 수 없음", Collectors.counting()));
                    List<Map.Entry<String, Long>> top3 = countByName.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(3)
                            .toList();
                    if (top3.isEmpty()) return report("최근 3개월간 휴재 신청이 없습니다.");
                    StringBuilder sb = new StringBuilder();
                    sb.append("최근 3개월간 휴재 신청이 많은 작가 TOP 3입니다.\n");
                    for (int i = 0; i < top3.size(); i++) {
                        sb.append((i + 1)).append(". ").append(top3.get(i).getKey()).append(" ").append(top3.get(i).getValue()).append("건\n");
                    }
                    return report(sb.toString().trim());
                }
                if ("pending_approvals".equals(t)) {
                    List<AttendanceRequestResponse> requests;
                    try {
                        requests = attendanceService.getAttendanceRequestsByManager(memberNo);
                    } catch (Exception e) {
                        log.warn("getAttendanceRequestsByManager 실패: {}", e.getMessage());
                        return report("근태 신청 목록을 불러오는 중 오류가 발생했습니다.");
                    }
                    long pending = requests != null ? requests.stream().filter(r -> r != null && PENDING.equals(r.getAttendanceRequestStatus())).count() : 0;
                    if (pending == 0) return report("현재 승인 대기 중인 근태 신청이 없습니다.");
                    return report(String.format("현재 승인 대기 중인 근태 신청이 %d건 있습니다. 대시보드에서 확인해 주세요.", pending));
                }
            }

            // ----- 에이전시 관리자(Agency) -----
            Long agencyNo = memberRepository.findByIdWithAgency(memberNo)
                    .filter(m -> m.getAgency() != null)
                    .map(m -> m.getAgency().getAgencyNo())
                    .orElse(null);
            if (agencyNo != null) {
                if ("company_compliance".equals(t)) {
                    AgencyDashboardMetricsResponse metrics = agencyService.getDashboardMetrics(agencyNo);
                    ComplianceTrendResponse trend = agencyService.getComplianceTrend(agencyNo);
                    if (metrics == null) return report("전사 마감 준수율 데이터가 없습니다.");
                    double rate = metrics.getAverageDeadlineComplianceRate() != null ? metrics.getAverageDeadlineComplianceRate() : 0;
                    String change = "";
                    if (trend != null && trend.getMonthOverMonthChange() != null) {
                        double c = trend.getMonthOverMonthChange();
                        change = c >= 0 ? String.format(" (전월 대비 +%.1f%%)", c) : String.format(" (전월 대비 %.1f%%)", c);
                    }
                    return report(String.format("전사 평균 마감 준수율은 %.1f%%입니다.%s", rate, change));
                }
                if ("at_risk_projects".equals(t)) {
                    List<AgencyDeadlineCountResponse.DeadlineItem> deadlineItems = agencyService.getAgencyDeadlineCounts(agencyNo);
                    if (deadlineItems == null || deadlineItems.isEmpty()) return report("현재 마감 임박 데이터가 없습니다.");
                    StringBuilder sb = new StringBuilder();
                    sb.append("⚠️ 운영 주의: 마감 임박 현황입니다. 준수율·휴재는 대시보드에서 확인해 주세요.\n");
                    for (AgencyDeadlineCountResponse.DeadlineItem d : deadlineItems) {
                        if (d.getCount() > 0) sb.append("• ").append(d.getName()).append(": ").append(d.getCount()).append("건\n");
                    }
                    return report(sb.toString().trim());
                }
                if ("join_approval_requests".equals(t)) {
                    var joinList = agencyService.getJoinRequests(agencyNo);
                    int joinCount = joinList != null ? (int) joinList.stream().filter(j -> "대기".equals(j.getNewRequestStatus())).count() : 0;
                    List<AttendanceRequestResponse> pendingLeave = attendanceService.getPendingAttendanceRequestsByAgency(agencyNo);
                    int leaveCount = pendingLeave != null ? pendingLeave.size() : 0;
                    return report(String.format("에이전시 가입 대기 %d건, 근태 결재 대기 %d건입니다. 요청 관리에서 확인해 주세요.", joinCount, leaveCount));
                }
                if ("health_distribution".equals(t)) {
                    HealthDistributionResponse dist = agencyService.getHealthDistribution(agencyNo);
                    if (dist == null || dist.getMentalDistribution() == null) return report("건강 분포 데이터가 없습니다.");
                    long total = dist.getMentalDistribution().stream().mapToLong(HealthDistributionResponse.HealthItem::getValue).sum();
                    long caution = dist.getMentalDistribution().stream().filter(i -> "주의".equals(i.getName())).mapToLong(HealthDistributionResponse.HealthItem::getValue).sum();
                    if (total == 0) return report("직원 건강 분포 데이터가 없습니다.");
                    int pct = (int) Math.round(100.0 * caution / total);
                    return report(String.format("전체 직원의 %d%%가 주의 단계입니다. 검진 독려가 필요합니다.", pct));
                }
            }

            return report("지원하지 않는 퀵 리포트 유형입니다.");
        } catch (Exception e) {
            log.warn("퀵 리포트 조회 실패: type={}, memberNo={}", type, memberNo, e);
            return report("데이터를 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}
