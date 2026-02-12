package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.agency.dto.response.AgencyDeadlineCountResponse;
import com.kh.magamGG.domain.agency.dto.response.AgencyDashboardMetricsResponse;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceRequestResponse;
import com.kh.magamGG.domain.attendance.dto.response.LeaveBalanceResponse;
import com.kh.magamGG.domain.attendance.service.AttendanceService;
import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.dto.ChatResponse.ChatAction;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.member.service.MemberService;
import com.kh.magamGG.domain.project.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.dto.response.TodayTaskResponse;
import com.kh.magamGG.domain.project.service.KanbanBoardService;
import com.kh.magamGG.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
}
