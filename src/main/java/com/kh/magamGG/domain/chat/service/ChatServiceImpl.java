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
import com.kh.magamGG.domain.chat.dto.ChatResponse.ChatAction;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String PENDING = "PENDING";

    /** 마감지기 DB 스키마 가이드 (AI 학습용) - 마감지기.sql 기반 */
    private static final String DB_SCHEMA_GUIDE = """
            [DB 스키마 가이드 - MagamGG 마감지기]
            - MEMBER: 회원. MEMBER_ROLE=웹툰작가/웹소설작가/어시스트(채색,조명,배경,선화,기타)/에이전시관리자/담당자. AGENCY_NO로 소속 에이전시.
            - AGENCY: 에이전시(스튜디오). AGENCY_LEAVE=기본연차일수.
            - PROJECT: 프로젝트. PROJECT_STATUS=연재/휴재/완결. PROJECT_CYCLE=연재주기.
            - PROJECT_MEMBER: 프로젝트 참여 인원. MEMBER_NO+PROJECT_NO. PROJECT_MEMBER_ROLE=LEADER/메인작가/어시.
            - KANBAN_BOARD: 칸반 보드(프로젝트별). KANBAN_CARD: 작업 카드. KANBAN_CARD_STATUS=Y(완료)/N(미완료)/D(삭제). KANBAN_CARD_ENDED_AT=마감일.
            - MANAGER: 담당자(MEMBER_NO→MANAGER_NO). ARTIST_ASSIGNMENT: 담당자-작가 배정(ARTIST_MEMBER_NO, MANAGER_NO).
            - ATTENDANCE_REQUEST: 근태 신청. TYPE=연차/병가/워케이션/재택/휴재/반차/반반차. STATUS=PENDING/승인/반려/취소.
            - LEAVE_BALANCE: 연차 잔액. LEAVE_BALANCE_REMAIN_DAYS=잔여일. LEAVE_TYPE=연차/대체휴무/특별휴가.
            - NEW_REQUEST: 에이전시 가입 요청. STATUS=승인/대기/거절.
            - 프로젝트 참여 인원 수 = PROJECT_MEMBER에서 해당 PROJECT_NO의 행 개수.
            """;

    private final OllamaService ollamaService;
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
        // 1. 컨텍스트 데이터 수집
        String context = buildContext(memberNo, request.getUserRole());

        // 2. 시스템 프롬프트 생성
        String systemPrompt = buildSystemPrompt(request.getUserRole(), context);

        // 3. 대화 히스토리 포함한 프롬프트 생성
        String fullPrompt = buildFullPrompt(request);

        // 4. Ollama 호출
        String aiResponse = ollamaService.generate(fullPrompt, systemPrompt);

        // 5. 액션 파싱 (페이지 이동 등)
        ChatAction action = parseAction(aiResponse, request.getUserRole());

        // 6. 액션 태그 제거한 메시지 반환
        String cleanMessage = removeActionTags(aiResponse);

        return ChatResponse.builder()
                .message(cleanMessage)
                .action(action)
                .build();
    }

    @Override
    public boolean isAIAvailable() {
        return ollamaService.isAvailable();
    }

    private String buildContext(Long memberNo, String userRole) {
        StringBuilder context = new StringBuilder();

        if (memberNo == null) {
            return context.toString();
        }

        try {
            String role = normalizeRole(userRole);

            switch (role) {
                case "artist" -> buildArtistContext(context, memberNo);
                case "manager" -> buildManagerContext(context, memberNo);
                case "agency" -> buildAgencyContext(context, memberNo);
                default -> buildDefaultContext(context, memberNo);
            }
        } catch (Exception e) {
            log.warn("컨텍스트 데이터 수집 실패: {}", e.getMessage());
        }

        return context.toString();
    }

    private String normalizeRole(String userRole) {
        if (userRole == null) return "artist";
        return switch (userRole) {
            case "담당자", "manager" -> "manager";
            case "에이전시 관리자", "agency" -> "agency";
            case "작가", "artist" -> "artist";
            default -> "artist";
        };
    }

    private void buildArtistContext(StringBuilder context, Long memberNo) {
        // 오늘 할 일 (DB 조회)
        List<TodayTaskResponse> todayTasks = kanbanBoardService.getTodayTasksForMember(memberNo);
        int taskCount = todayTasks != null ? todayTasks.size() : 0;
        context.append("오늘 할 일: ").append(taskCount).append("건\n");
        if (todayTasks != null && !todayTasks.isEmpty()) {
            for (TodayTaskResponse t : todayTasks) {
                context.append("  - [").append(t.getProjectName()).append("] ").append(t.getTitle());
                if (t.getDueDate() != null) context.append(" (마감: ").append(t.getDueDate()).append(")");
                context.append("\n");
            }
        }

        // 연차 잔여 (DB 조회)
        LeaveBalanceResponse leaveBalance = attendanceService.getLeaveBalance(memberNo);
        if (leaveBalance != null) {
            context.append("연차 잔여: ").append(leaveBalance.getLeaveBalanceRemainDays()).append("일\n");
        }
    }

    private void buildManagerContext(StringBuilder context, Long memberNo) {
        // 담당자 본인 정보: 오늘 할 일, 연차
        List<TodayTaskResponse> todayTasks = kanbanBoardService.getTodayTasksForMember(memberNo);
        int taskCount = todayTasks != null ? todayTasks.size() : 0;
        context.append("담당자 오늘 할 일: ").append(taskCount).append("건\n");

        LeaveBalanceResponse leaveBalance = attendanceService.getLeaveBalance(memberNo);
        if (leaveBalance != null) {
            context.append("연차 잔여: ").append(leaveBalance.getLeaveBalanceRemainDays()).append("일\n");
        }

        // 담당 작가 목록 (DB 조회) - "담당 작가 누구야" 질문에 답변용
        List<MemberResponse> assignedArtists = memberService.getAssignedArtistsByMemberNo(memberNo);
        if (assignedArtists != null && !assignedArtists.isEmpty()) {
            context.append("담당 작가: ").append(assignedArtists.size()).append("명 - ");
            context.append(assignedArtists.stream().map(MemberResponse::getMemberName).filter(n -> n != null).toList().toString()).append("\n");
        }

        // 담당 작가들의 근태 신청 (DB 조회)
        List<AttendanceRequestResponse> requests = attendanceService.getAttendanceRequestsByManager(memberNo);
        long pendingCount = requests != null ? requests.stream()
                .filter(r -> PENDING.equals(r.getAttendanceRequestStatus()))
                .count() : 0;
        context.append("담당 작가 근태 신청 대기: ").append(pendingCount).append("건\n");
        if (requests != null && pendingCount > 0) {
            List<AttendanceRequestResponse> pending = requests.stream()
                    .filter(r -> PENDING.equals(r.getAttendanceRequestStatus()))
                    .limit(5)
                    .toList();
            for (AttendanceRequestResponse r : pending) {
                context.append("  - ").append(r.getMemberName()).append(": ")
                        .append(r.getAttendanceRequestType()).append(" ")
                        .append(r.getAttendanceRequestUsingDays() != null ? r.getAttendanceRequestUsingDays() + "일" : "")
                        .append("\n");
            }
        }

        // 담당 프로젝트 현황 (DB 조회)
        List<ManagedProjectResponse> projects = projectService.getManagedProjectsByManager(memberNo);
        if (projects != null && !projects.isEmpty()) {
            context.append("담당 프로젝트: ").append(projects.size()).append("건\n");
            for (ManagedProjectResponse p : projects) {
                context.append("  - ").append(p.getProjectName()).append(" (").append(p.getArtist()).append(") ")
                        .append(p.getStatus()).append(", 진행률 ").append(p.getProgress()).append("%\n");
            }
        }

        // 마감 임박 (DB 조회)
        List<DeadlineCountResponse> deadlines = projectService.getDeadlineCountsForManager(memberNo);
        if (deadlines != null && !deadlines.isEmpty()) {
            context.append("마감 임박:\n");
            for (DeadlineCountResponse d : deadlines) {
                context.append("  - ").append(d.getName()).append(": ").append(d.getCount()).append("건\n");
            }
        }
    }

    private void buildAgencyContext(StringBuilder context, Long memberNo) {
        Long agencyNo = memberRepository.findByIdWithAgency(memberNo)
                .filter(m -> m.getAgency() != null)
                .map(m -> m.getAgency().getAgencyNo())
                .orElse(null);

        if (agencyNo == null) {
            context.append("에이전시 정보를 조회할 수 없습니다.\n");
            return;
        }

        // 담당자 목록 (DB 조회) - "담당자 몇명/이름" 질문에 답변용
        List<MemberResponse> managers = memberService.getManagersByAgencyNo(agencyNo);
        if (managers != null && !managers.isEmpty()) {
            context.append("담당자: ").append(managers.size()).append("명 - ");
            context.append(managers.stream().map(MemberResponse::getMemberName).filter(n -> n != null).toList().toString()).append("\n");
        }

        // 작가 목록 (DB 조회) - "직원/작가 이름" 질문에 답변용
        List<MemberResponse> artists = memberService.getArtistsByAgencyNo(agencyNo);
        if (artists != null && !artists.isEmpty()) {
            context.append("작가: ").append(artists.size()).append("명 - ");
            context.append(artists.stream().map(MemberResponse::getMemberName).filter(n -> n != null).toList().toString()).append("\n");
        }

        // 전체 직원 수 (DB 조회)
        List<MemberResponse> allMembers = memberService.getMembersByAgencyNo(agencyNo);
        if (allMembers != null) {
            context.append("전체 직원: ").append(allMembers.size()).append("명\n");
        }

        // 대기 중인 근태 신청 (DB 조회)
        List<AttendanceRequestResponse> pendingAttendance = attendanceService.getPendingAttendanceRequestsByAgency(agencyNo);
        int pendingCount = pendingAttendance != null ? pendingAttendance.size() : 0;
        context.append("근태 신청 대기: ").append(pendingCount).append("건\n");
        if (pendingAttendance != null && !pendingAttendance.isEmpty()) {
            for (AttendanceRequestResponse r : pendingAttendance.stream().limit(5).toList()) {
                context.append("  - ").append(r.getMemberName()).append(": ")
                        .append(r.getAttendanceRequestType()).append(" ")
                        .append(r.getAttendanceRequestUsingDays() != null ? r.getAttendanceRequestUsingDays() + "일" : "")
                        .append("\n");
            }
        }

        // 가입 요청 (DB 조회)
        var joinRequests = agencyService.getJoinRequests(agencyNo);
        int joinCount = joinRequests != null ? joinRequests.size() : 0;
        context.append("가입 요청 대기: ").append(joinCount).append("건\n");

        // 대시보드 메트릭 (DB 조회)
        AgencyDashboardMetricsResponse metrics = agencyService.getDashboardMetrics(agencyNo);
        if (metrics != null) {
            context.append("활동 작가: ").append(metrics.getActiveArtistCount() != null ? metrics.getActiveArtistCount() : 0).append("명\n");
            context.append("진행 프로젝트: ").append(metrics.getActiveProjectCount() != null ? metrics.getActiveProjectCount() : 0).append("건\n");
            if (metrics.getAverageDeadlineComplianceRate() != null) {
                context.append("평균 마감 준수율: ").append(String.format("%.1f", metrics.getAverageDeadlineComplianceRate())).append("%\n");
            }
        }

        // 마감 임박 (DB 조회)
        List<AgencyDeadlineCountResponse.DeadlineItem> deadlineItems = agencyService.getAgencyDeadlineCounts(agencyNo);
        if (deadlineItems != null && !deadlineItems.isEmpty()) {
            context.append("마감 임박:\n");
            for (AgencyDeadlineCountResponse.DeadlineItem d : deadlineItems) {
                context.append("  - ").append(d.getName()).append(": ").append(d.getCount()).append("건\n");
            }
        }
    }

    private void buildDefaultContext(StringBuilder context, Long memberNo) {
        List<TodayTaskResponse> todayTasks = kanbanBoardService.getTodayTasksForMember(memberNo);
        int taskCount = todayTasks != null ? todayTasks.size() : 0;
        context.append("오늘 할 일: ").append(taskCount).append("건\n");

        LeaveBalanceResponse leaveBalance = attendanceService.getLeaveBalance(memberNo);
        if (leaveBalance != null) {
            context.append("연차 잔여: ").append(leaveBalance.getLeaveBalanceRemainDays()).append("일\n");
        }
    }

    private String buildSystemPrompt(String userRole, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 MagamGG 시스템의 AI 어시스턴트 '지지'입니다.\n");
        prompt.append("웹툰 작가 관리 시스템(마감지기)에서 사용자를 돕습니다.\n\n");
        prompt.append(DB_SCHEMA_GUIDE).append("\n");
        prompt.append("응답 규칙:\n");
        prompt.append("1. 간결하고 친근하게 답변하세요 (~요 체 사용)\n");
        prompt.append("2. 페이지 안내 시 [ACTION:페이지명] 형식으로 끝에 추가하세요\n");
        prompt.append("3. [금지] 아래 '현재 데이터'에 없는 숫자(예: 3명, 4명), 이름, 날짜는 절대 만들지 마세요. 그런 답변은 버그입니다.\n");
        prompt.append("4. [필수] 답변에 사용할 정보는 오직 '현재 데이터'에 적힌 것만 사용하세요. 데이터에 있으면 그대로 인용하세요.\n");
        prompt.append("5. [필수] 데이터에 없는 질문(직원 이름 목록, 프로젝트 참여 인원, 과거 내역 등)이면 반드시 이렇게만 답하세요: '해당 정보는 전체 직원(또는 대시보드) 페이지에서 직접 확인해 주세요.'\n");
        prompt.append("6. 올바른 예: 데이터에 '담당자: 2명 - [홍길동, 김철수]'가 있으면 '담당자는 2명이에요. 홍길동님, 김철수님이세요.'라고 답할 수 있음.\n");
        prompt.append("7. 잘못된 예: 데이터에 없는 '3명', '4명', '대시보드에서 기록된 직원' 같은 표현은 절대 사용하지 마세요.\n");
        prompt.append("8. 한국어로만 답변하세요.\n\n");

        // 역할별 페이지 안내
        prompt.append("현재 사용자 역할: ");
        switch (userRole) {
            case "artist":
            case "작가":
                prompt.append("작가\n");
                prompt.append("접근 가능 페이지: 대시보드, 프로젝트 관리, 캘린더, 건강관리\n");
                prompt.append("휴가 신청: 상단 헤더의 '근태 신청' 버튼 사용 [ACTION:근태신청]\n");
                prompt.append("(원격 관리 페이지는 작가에게 없습니다)\n");
                break;
            case "manager":
            case "담당자":
                prompt.append("담당자\n");
                prompt.append("접근 가능 페이지: 대시보드, 프로젝트 관리, 캘린더, 직원 관리, 원격 관리, 건강 검사, 작가 건강관리\n");
                prompt.append("휴가 신청: 상단 헤더의 '근태 신청' 버튼 사용 [ACTION:근태신청]\n");
                break;
            case "agency":
            case "에이전시 관리자":
                prompt.append("에이전시 관리자\n");
                prompt.append("접근 가능 페이지: 대시보드, 전체 프로젝트, 전체 직원, 요청 관리, 건강관리, 원격 관리, 할당 관리, 연차 설정\n");
                break;
            default:
                prompt.append("알 수 없음\n");
        }

        String ctx = context.toString();
        prompt.append("\n현재 데이터:\n").append(ctx.isBlank() ? "(없음 - 모든 구체적 질문에는 '해당 페이지에서 직접 확인해 주세요'로 답하세요)" : ctx);

        return prompt.toString();
    }

    private String buildFullPrompt(ChatRequest request) {
        StringBuilder prompt = new StringBuilder();

        // 대화 히스토리 추가 (최대 5개)
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            int start = Math.max(0, request.getHistory().size() - 5);
            for (int i = start; i < request.getHistory().size(); i++) {
                var msg = request.getHistory().get(i);
                if ("user".equals(msg.getRole())) {
                    prompt.append("사용자: ").append(msg.getContent()).append("\n");
                } else {
                    prompt.append("지지: ").append(msg.getContent()).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("사용자: ").append(request.getMessage()).append("\n");
        prompt.append("지지: ");

        return prompt.toString();
    }

    private ChatAction parseAction(String response, String userRole) {
        // [ACTION:페이지명] 패턴 검색
        Pattern pattern = Pattern.compile("\\[ACTION:(.*?)\\]");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String pageName = matcher.group(1).trim();

            // 근태 신청은 특별 처리
            if (pageName.contains("근태") || pageName.contains("휴가") || pageName.contains("신청")) {
                return ChatAction.builder()
                        .actionType("attendance")
                        .actionLabel("근태 신청 열기")
                        .build();
            }

            // 페이지 이동
            return ChatAction.builder()
                    .actionType("section")
                    .actionLabel(pageName + " 이동")
                    .sectionKeyword(pageName)
                    .build();
        }

        return null;
    }

    private String removeActionTags(String response) {
        return response.replaceAll("\\[ACTION:.*?\\]", "").trim();
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
                HealthSurveyResponseStatusResponse latest = null;
                if (mental != null && mental.isCompleted() && mental.getLastCheckDate() != null) latest = mental;
                if (physical != null && physical.isCompleted() && physical.getLastCheckDate() != null) {
                    if (latest == null || physical.getLastCheckDate().isAfter(latest.getLastCheckDate())) latest = physical;
                }
                if (latest == null) return report("검진 결과를 불러올 수 없습니다.");
                String dateStr = latest.getLastCheckDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                int score = latest.getTotalScore() != null ? latest.getTotalScore() : 0;
                String level = latest.getRiskLevel() != null ? latest.getRiskLevel() : "확인 필요";
                return report(String.format("가장 최근 검진: %s\n총점 %d점, 상태: %s입니다.", dateStr, score, level));
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
