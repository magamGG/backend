package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.attendance.service.AttendanceService;
import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.dto.ChatResponse.ChatAction;
import com.kh.magamGG.domain.project.service.KanbanBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final OllamaService ollamaService;
    private final KanbanBoardService kanbanBoardService;
    private final AttendanceService attendanceService;

    // 역할별 페이지 정보
    private static final Map<String, String> INDIVIDUAL_PAGES = Map.of(
            "대시보드", "대시보드",
            "프로젝트", "프로젝트 관리",
            "캘린더", "캘린더",
            "건강관리", "건강관리"
    );

    private static final Map<String, String> MANAGER_PAGES = Map.of(
            "대시보드", "대시보드",
            "프로젝트", "프로젝트 관리",
            "캘린더", "캘린더",
            "직원 관리", "직원 관리",
            "원격 관리", "원격 관리",
            "건강 검사", "건강 검사",
            "작가 건강관리", "작가 건강관리"
    );

    private static final Map<String, String> AGENCY_PAGES = Map.of(
            "대시보드", "대시보드",
            "전체 프로젝트", "프로젝트",
            "전체 직원", "직원",
            "요청 관리", "요청",
            "건강관리", "건강",
            "원격 관리", "원격",
            "할당 관리", "할당",
            "연차 설정", "연차"
    );

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

        try {
            // 오늘 할 일 개수
            if (memberNo != null) {
                var todayTasks = kanbanBoardService.getTodayTasksForMember(memberNo);
                int taskCount = todayTasks != null ? todayTasks.size() : 0;
                context.append("오늘 할 일: ").append(taskCount).append("건\n");
            }

            // 연차 잔여 (로그인한 경우)
            if (memberNo != null) {
                try {
                    var leaveBalance = attendanceService.getLeaveBalance(memberNo);
                    if (leaveBalance != null) {
                        context.append("연차 잔여: ").append(leaveBalance.getLeaveBalanceRemainDays()).append("일\n");
                    }
                } catch (Exception e) {
                    // 연차 정보 없음
                }
            }
        } catch (Exception e) {
            log.warn("컨텍스트 데이터 수집 실패: {}", e.getMessage());
        }

        return context.toString();
    }

    private String buildSystemPrompt(String userRole, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 MagamGG 시스템의 AI 어시스턴트 '지지'입니다.\n");
        prompt.append("웹툰 작가 관리 시스템에서 사용자를 돕습니다.\n\n");

        prompt.append("응답 규칙:\n");
        prompt.append("1. 간결하고 친근하게 답변하세요 (~요 체 사용)\n");
        prompt.append("2. 페이지 안내 시 [ACTION:페이지명] 형식으로 끝에 추가하세요\n");
        prompt.append("3. 모르는 것은 솔직히 '해당 정보는 직접 확인이 필요해요'라고 답하세요\n");
        prompt.append("4. 숫자 정보는 '현재 데이터'에 있는 것만 정확히 전달하세요\n");
        prompt.append("5. 한국어로만 답변하세요\n");
        prompt.append("6. [중요] 직원 이름, 프로젝트명, 구체적인 데이터는 절대 만들어내지 마세요\n");
        prompt.append("7. [중요] DB 조회가 필요한 질문은 '해당 페이지에서 직접 확인해주세요'라고 안내하세요\n");
        prompt.append("8. 당신이 알 수 있는 정보: 오늘 할 일 개수, 연차 잔여일 (현재 데이터에 있는 것만)\n");
        prompt.append("9. 당신이 모르는 정보: 직원 목록, 프로젝트 상세, 결재 내역, 구체적인 이름/날짜 등\n\n");

        // 역할별 페이지 안내
        prompt.append("현재 사용자 역할: ");
        switch (userRole) {
            case "individual":
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

        prompt.append("\n현재 데이터:\n").append(context);

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
