package com.kh.magamGG.domain.ai.service;

import com.kh.magamGG.domain.ai.context.ToneContext;
import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.repository.AttendanceRequestRepository;
import com.kh.magamGG.domain.attendance.repository.LeaveBalanceRepository;
import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import com.kh.magamGG.domain.health.entity.HealthSurveyResponseItem;
import com.kh.magamGG.domain.health.repository.DailyHealthCheckRepository;
import com.kh.magamGG.domain.health.repository.HealthSurveyResponseItemRepository;
import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagamjigiAiService {

    private final ChatModel chatModel;
    private final MemberRepository memberRepository;
    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final HealthSurveyResponseItemRepository healthSurveyResponseItemRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final DailyHealthCheckRepository dailyHealthCheckRepository;
    private final ProjectRepository projectRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;

    private static final List<String> ARTIST_KEYWORDS = List.of("작가", "어시스트");

    public String getArtistHealthFeedback(Long memberNo) {
        validateArtistRole(memberNo);

        int phq9 = getLatestScoreByOrderRange(memberNo, "월간 정신", 1, 9);
        int gad = getLatestScoreByOrderRange(memberNo, "월간 정신", 10, 19);
        int dash = getLatestTotalScore(memberNo, "월간 신체");

        String templateText =
            "너는 마감지기를 사용하는 웹툰/웹소설 작가의 건강을 챙겨주는 따뜻한 매니저야. " +
            "작가의 최근 건강 설문 결과를 3가지 검사별로 알려줄게.\n" +
            "1) 우울 지수(PHQ-9): {phq9}점 (27점 만점, 10점 이상이면 주의)\n" +
            "2) 불안 지수(GAD): {gad}점 (40점 만점, 16점 이상이면 주의)\n" +
            "3) 손목/어깨 통증 지수(QuickDASH): {dash}점 (55점 만점, 25점 이상이면 주의)\n\n" +
            "각 검사 결과를 개별적으로 분석해서, " +
            "의학적 진단은 절대 배제하고, 현재 점수 상태에 대한 공감과 함께 " +
            "일상이나 작업 중에 할 수 있는 가벼운 스트레칭, 마인드컨트롤 팁을 " +
            "친근하고 부드러운 말투로 조언해 줘. " +
            "각 검사별 1~2문장, 총 5문장 이내로 작성해.";

        return callAi(templateText, Map.of("phq9", phq9, "gad", gad, "dash", dash));
    }

    public String getManagerArtistHealthSummary(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        Manager manager = managerRepository.findByMember_MemberNo(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자 정보를 찾을 수 없습니다."));
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());

        if (assignments.isEmpty()) {
            return "현재 배정된 작가가 없습니다.";
        }

        StringBuilder artistData = new StringBuilder();
        for (ArtistAssignment assignment : assignments) {
            Member artist = assignment.getArtist();
            Long mNo = artist.getMemberNo();
            int phq9 = getLatestScoreByOrderRange(mNo, "월간 정신", 1, 9);
            int gad = getLatestScoreByOrderRange(mNo, "월간 정신", 10, 19);
            int dash = getLatestTotalScore(mNo, "월간 신체");
            artistData.append(String.format("- %s(%s): PHQ-9 %d점(%s), GAD %d점(%s), QuickDASH %d점(%s)\n",
                artist.getMemberName(), artist.getMemberRole(),
                phq9, evaluateRiskPhq9(phq9), gad, evaluateRiskGad(gad), dash, evaluateRiskDash(dash)));
        }

        String templateText =
            "너는 웹툰/웹소설 에이전시의 담당자를 돕는 AI 어시스턴트야. " +
            "담당자가 관리하는 작가 {count}명의 최근 건강 현황이야:\n{artistData}\n" +
            "PHQ-9은 우울 지수(27점 만점), GAD는 불안 지수(40점 만점), QuickDASH는 손목/어깨 통증 지수(55점 만점)야.\n" +
            "각 작가의 상태를 요약하고, '주의' 이상인 작가에 대해 " +
            "업무 배분 조정이나 휴식 권유 등 관리 차원의 조언을 3~5문장으로 해 줘. " +
            "의학적 진단은 절대 배제해.";

        return callAi(templateText, Map.of(
            "count", assignments.size(),
            "artistData", artistData.toString()
        ));
    }

    public String getAgencyHealthOverview(Long memberNo) {
        validateExactRole(memberNo, "에이전시 관리자");

        Member admin = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Long agencyNo = admin.getAgency().getAgencyNo();

        List<HealthSurveyResponseItem> allItems =
            healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);

        Map<Long, List<HealthSurveyResponseItem>> byMember = allItems.stream()
            .collect(Collectors.groupingBy(i -> i.getMember().getMemberNo()));

        int totalMembers = byMember.size();
        int phq9Caution = 0, phq9Warning = 0, phq9Danger = 0;
        int gadCaution = 0, gadWarning = 0, gadDanger = 0;
        int dashCaution = 0, dashWarning = 0, dashDanger = 0;

        for (Map.Entry<Long, List<HealthSurveyResponseItem>> entry : byMember.entrySet()) {
            Long mNo = entry.getKey();
            int phq9 = getLatestScoreByOrderRange(mNo, "월간 정신", 1, 9);
            int gad = getLatestScoreByOrderRange(mNo, "월간 정신", 10, 19);
            int dash = getLatestTotalScore(mNo, "월간 신체");

            String rP = evaluateRiskPhq9(phq9);
            if ("주의".equals(rP)) phq9Caution++;
            else if ("경고".equals(rP)) phq9Warning++;
            else if ("위험".equals(rP)) phq9Danger++;

            String rG = evaluateRiskGad(gad);
            if ("주의".equals(rG)) gadCaution++;
            else if ("경고".equals(rG)) gadWarning++;
            else if ("위험".equals(rG)) gadDanger++;

            String rD = evaluateRiskDash(dash);
            if ("주의".equals(rD)) dashCaution++;
            else if ("경고".equals(rD)) dashWarning++;
            else if ("위험".equals(rD)) dashDanger++;
        }

        String templateText =
            "너는 웹툰/웹소설 에이전시의 경영진을 돕는 AI 어시스턴트야. " +
            "에이전시 소속 {total}명의 건강 현황 요약이야:\n" +
            "[우울(PHQ-9)] 주의 {pc1}명, 경고 {pw1}명, 위험 {pd1}명\n" +
            "[불안(GAD)] 주의 {gc1}명, 경고 {gw1}명, 위험 {gd1}명\n" +
            "[신체(QuickDASH)] 주의 {dc1}명, 경고 {dw1}명, 위험 {dd1}명\n\n" +
            "전체적인 조직 건강 상태를 3가지 영역별로 분석하고, " +
            "복지 정책, 업무량 조정, 건강 검진 독려 등 " +
            "조직 운영 차원의 제안을 3~5문장으로 해 줘. " +
            "의학적 진단은 절대 배제하고, 긍정적이고 건설적인 톤으로 작성해.";

        return callAi(templateText, Map.of(
            "total", totalMembers,
            "pc1", phq9Caution, "pw1", phq9Warning, "pd1", phq9Danger,
            "gc1", gadCaution, "gw1", gadWarning, "gd1", gadDanger,
            "dc1", dashCaution, "dw1", dashWarning, "dd1", dashDanger
        ));
    }

    /**
     * 에이전시 관리자: 프로젝트·작가 리스크 종합 — 마감 준수율 낮은 프로젝트/작가, 건강 위험/경고 인원 요약 후
     * "지금 가장 손봐야 할 프로젝트/팀 1~2개와 이유"를 짧게 제안 (우선순위용).
     */
    public String getAgencyRiskSummary(Long memberNo) {
        validateExactRole(memberNo, "에이전시 관리자");

        Member admin = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Long agencyNo = admin.getAgency().getAgencyNo();
        LocalDate today = LocalDate.now();

        List<Project> projects = projectRepository.findAllProjectsByAgencyNo(agencyNo);
        List<String> lowComplianceProjects = new ArrayList<>();
        for (Project project : projects) {
            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            long pastDueY = 0, pastDueN = 0;
            for (KanbanCard c : cards) {
                if (c.getKanbanCardEndedAt() == null || c.getKanbanCardEndedAt().isAfter(today)) continue;
                if ("Y".equals(c.getKanbanCardStatus())) pastDueY++;
                else if ("N".equals(c.getKanbanCardStatus())) pastDueN++;
            }
            long total = pastDueY + pastDueN;
            if (total >= 1) {
                double compliance = pastDueY * 100.0 / total;
                if (compliance < 80.0) {
                    lowComplianceProjects.add(String.format("%s: 마감 준수율 %.0f%%", project.getProjectName(), compliance));
                }
            }
        }

        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        List<String> lowComplianceArtists = new ArrayList<>();
        for (Member artist : artists) {
            long pastDueY = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(
                artist.getMemberNo(), today, "Y");
            long pastDueN = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(
                artist.getMemberNo(), today, "N");
            long total = pastDueY + pastDueN;
            if (total >= 1) {
                double compliance = pastDueY * 100.0 / total;
                if (compliance < 80.0) {
                    lowComplianceArtists.add(String.format("%s(%s): %.0f%%", artist.getMemberName(), artist.getMemberRole(), compliance));
                }
            }
        }

        List<HealthSurveyResponseItem> allItems = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);
        Map<Long, List<HealthSurveyResponseItem>> byMember = allItems.stream()
            .collect(Collectors.groupingBy(i -> i.getMember().getMemberNo()));
        int phq9Risk = 0, gadRisk = 0, dashRisk = 0;
        for (Map.Entry<Long, List<HealthSurveyResponseItem>> entry : byMember.entrySet()) {
            int phq9 = getLatestScoreByOrderRange(entry.getKey(), "월간 정신", 1, 9);
            int gad = getLatestScoreByOrderRange(entry.getKey(), "월간 정신", 10, 19);
            int dash = getLatestTotalScore(entry.getKey(), "월간 신체");
            if ("경고".equals(evaluateRiskPhq9(phq9)) || "위험".equals(evaluateRiskPhq9(phq9))) phq9Risk++;
            if ("경고".equals(evaluateRiskGad(gad)) || "위험".equals(evaluateRiskGad(gad))) gadRisk++;
            if ("경고".equals(evaluateRiskDash(dash)) || "위험".equals(evaluateRiskDash(dash))) dashRisk++;
        }
        String healthSummary = String.format("건강 위험/경고: 우울 %d명, 불안 %d명, 신체 %d명", phq9Risk, gadRisk, dashRisk);

        String projectLines = lowComplianceProjects.isEmpty() ? "없음" : String.join("\n", lowComplianceProjects);
        String artistLines = lowComplianceArtists.isEmpty() ? "없음" : String.join("\n", lowComplianceArtists);

        String templateText =
            "너는 웹툰/웹소설 에이전시의 경영진을 돕는 AI 어시스턴트야. " +
            "대시보드에는 분포만 있으니, 아래 데이터를 바탕으로 '우선순위'를 주는 요약을 해 줘.\n\n" +
            "[마감 준수율이 낮은 프로젝트]\n{lowProjects}\n\n" +
            "[마감 준수율이 낮은 작가]\n{lowArtists}\n\n" +
            "[건강 현황]\n{healthSummary}\n\n" +
            "위를 종합해서, 지금 가장 손봐야 할 프로젝트 또는 팀 1~2개와 그 이유를 짧게(3~5문장) 제안해 줘.";

        return callAi(templateText, Map.of(
            "lowProjects", projectLines,
            "lowArtists", artistLines,
            "healthSummary", healthSummary
        ));
    }

    /**
     * 에이전시 관리자: 업무 마감률·연차·병가·건강 현황을 바탕으로 프로젝트 진행 리스크를 종합 분석.
     */
    public String getAgencyLeaveOverlapAlert(Long memberNo) {
        validateExactRole(memberNo, "에이전시 관리자");

        Member admin = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Long agencyNo = admin.getAgency().getAgencyNo();
        LocalDate today = LocalDate.now();
        LocalDate from = today;
        LocalDate to = from.plusDays(60);

        // 1) 마감 준수율: 낮은 프로젝트·작가
        List<Project> projects = projectRepository.findAllProjectsByAgencyNo(agencyNo);
        List<String> lowComplianceProjects = new ArrayList<>();
        for (Project project : projects) {
            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            long pastDueY = 0, pastDueN = 0;
            for (KanbanCard c : cards) {
                if (c.getKanbanCardEndedAt() == null || c.getKanbanCardEndedAt().isAfter(today)) continue;
                if ("Y".equals(c.getKanbanCardStatus())) pastDueY++;
                else if ("N".equals(c.getKanbanCardStatus())) pastDueN++;
            }
            long total = pastDueY + pastDueN;
            if (total >= 1) {
                double compliance = pastDueY * 100.0 / total;
                if (compliance < 80.0) {
                    lowComplianceProjects.add(String.format("%s: %.0f%%", project.getProjectName(), compliance));
                }
            }
        }
        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        List<String> lowComplianceArtists = new ArrayList<>();
        for (Member artist : artists) {
            long pastDueY = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(
                artist.getMemberNo(), today, "Y");
            long pastDueN = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(
                artist.getMemberNo(), today, "N");
            long total = pastDueY + pastDueN;
            if (total >= 1) {
                double compliance = pastDueY * 100.0 / total;
                if (compliance < 80.0) {
                    lowComplianceArtists.add(String.format("%s(%s): %.0f%%", artist.getMemberName(), artist.getMemberRole(), compliance));
                }
            }
        }
        String complianceSection = "[마감 준수율 80% 미만]\n프로젝트: " +
            (lowComplianceProjects.isEmpty() ? "없음" : String.join(", ", lowComplianceProjects)) +
            "\n작가: " + (lowComplianceArtists.isEmpty() ? "없음" : String.join(", ", lowComplianceArtists));

        // 2) 연차: 향후 60일 중 겹치는 날
        List<AttendanceRequest> approved = attendanceRequestRepository.findApprovedByAgencyNoAndDateRange(
            agencyNo, from.atStartOfDay(), to.atTime(23, 59, 59));
        Map<LocalDate, List<String>> dateToLeaveNames = new TreeMap<>();
        for (AttendanceRequest ar : approved) {
            if (!"연차".equals(ar.getAttendanceRequestType())) continue;
            LocalDate start = ar.getAttendanceRequestStartDate().toLocalDate();
            LocalDate end = ar.getAttendanceRequestEndDate().toLocalDate();
            String name = ar.getMember().getMemberName() + "(" + ar.getMember().getMemberRole() + ")";
            for (long d = 0; d <= ChronoUnit.DAYS.between(start, end); d++) {
                LocalDate day = start.plusDays(d);
                if (!day.isBefore(from) && !day.isAfter(to)) {
                    dateToLeaveNames.computeIfAbsent(day, k -> new ArrayList<>()).add(name);
                }
            }
        }
        List<String> overlapLines = new ArrayList<>();
        for (Map.Entry<LocalDate, List<String>> e : dateToLeaveNames.entrySet()) {
            if (e.getValue().size() >= 2) {
                overlapLines.add(String.format("%s: %d명 (%s)", e.getKey(), e.getValue().size(), String.join(", ", e.getValue())));
            }
        }
        String leaveSection = "[연차 겹침(향후 60일, 2명 이상 부재)]\n" +
            (overlapLines.isEmpty() ? "없음" : String.join("\n", overlapLines));

        // 3) 병가: 향후 60일 승인된 병가
        List<String> sickLeaveLines = new ArrayList<>();
        for (AttendanceRequest ar : approved) {
            if (!"병가".equals(ar.getAttendanceRequestType())) continue;
            LocalDate start = ar.getAttendanceRequestStartDate().toLocalDate();
            LocalDate end = ar.getAttendanceRequestEndDate().toLocalDate();
            String name = ar.getMember().getMemberName() + "(" + ar.getMember().getMemberRole() + ")";
            sickLeaveLines.add(String.format("%s ~ %s: %s", start, end, name));
        }
        String sickSection = "[병가(승인, 향후 60일)]\n" +
            (sickLeaveLines.isEmpty() ? "없음" : String.join("\n", sickLeaveLines));

        // 4) 건강: 경고/위험 인원 수
        List<HealthSurveyResponseItem> allItems = healthSurveyResponseItemRepository.findByAgencyNoWithSurvey(agencyNo);
        Map<Long, List<HealthSurveyResponseItem>> byMember = allItems.stream()
            .collect(Collectors.groupingBy(i -> i.getMember().getMemberNo()));
        int phq9Risk = 0, gadRisk = 0, dashRisk = 0;
        for (Map.Entry<Long, List<HealthSurveyResponseItem>> entry : byMember.entrySet()) {
            int phq9 = getLatestScoreByOrderRange(entry.getKey(), "월간 정신", 1, 9);
            int gad = getLatestScoreByOrderRange(entry.getKey(), "월간 정신", 10, 19);
            int dash = getLatestTotalScore(entry.getKey(), "월간 신체");
            if ("경고".equals(evaluateRiskPhq9(phq9)) || "위험".equals(evaluateRiskPhq9(phq9))) phq9Risk++;
            if ("경고".equals(evaluateRiskGad(gad)) || "위험".equals(evaluateRiskGad(gad))) gadRisk++;
            if ("경고".equals(evaluateRiskDash(dash)) || "위험".equals(evaluateRiskDash(dash))) dashRisk++;
        }
        String healthSection = "[건강 위험/경고 인원]\n우울(PHQ-9): " + phq9Risk + "명, 불안(GAD): " + gadRisk + "명, 신체(QuickDASH): " + dashRisk + "명";

        String templateText =
            "너는 웹툰/웹소설 에이전시의 경영진을 돕는 AI 어시스턴트야. " +
            "아래는 에이전시의 업무 마감률·연차·병가·건강 현황 요약이야.\n\n" +
            "{complianceSection}\n\n{leaveSection}\n\n{sickSection}\n\n{healthSection}\n\n" +
            "위 네 가지(마감률, 연차 상태, 병가, 건강)를 바탕으로 프로젝트 진행에 리스크가 될 수 있는 요인을 종합 분석해 줘. " +
            "우선순위가 높은 리스크와 대응 방향을 4~6문장으로 제안해 줘. " +
            "데이터가 없거나 양호한 항목도 있으면 그대로 언급한 뒤, 있는 항목만 강조해도 돼.";

        return callAi(templateText, Map.of(
            "complianceSection", complianceSection,
            "leaveSection", leaveSection,
            "sickSection", sickSection,
            "healthSection", healthSection
        ));
    }

    /**
     * 에이전시 관리자: 담당자별 작가 배정 현황 — 특정 담당자에게 작가가 몰려 있는지 분석.
     */
    public String getAgencyArtistAssignmentBalance(Long memberNo) {
        validateExactRole(memberNo, "에이전시 관리자");

        Member admin = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Long agencyNo = admin.getAgency().getAgencyNo();

        List<Manager> managers = managerRepository.findByAgencyNo(agencyNo);
        if (managers.isEmpty()) {
            return "해당 에이전시에 담당자가 없습니다.";
        }

        List<String> lines = new ArrayList<>();
        int totalArtists = 0;
        for (Manager m : managers) {
            int count = artistAssignmentRepository.findByManagerNo(m.getManagerNo()).size();
            totalArtists += count;
            String name = m.getMember() != null ? m.getMember().getMemberName() : "담당자#" + m.getManagerNo();
            lines.add(String.format("%s: %d명", name, count));
        }
        double avg = (double) totalArtists / managers.size();
        String assignmentText = String.join("\n", lines) + "\n(전체 " + totalArtists + "명 / 담당자당 평균 " + String.format("%.1f", avg) + "명)";

        String templateText =
            "너는 웹툰/웹소설 에이전시의 경영진을 돕는 AI 어시스턴트야. " +
            "담당자별 작가 배정 현황이야:\n\n{assignmentText}\n\n" +
            "특정 담당자에게 작가가 몰려 있는지, 배정이 고르지 않은 담당자가 있는지 분석해 줘. " +
            "균형 조정이 필요하면 2~4문장으로 제안해 줘. 고르게 배정되어 있으면 그렇게 요약해 줘.";

        return callAi(templateText, Map.of("assignmentText", assignmentText));
    }

    /**
     * 에이전시 관리자: 휴가·근태 신청이 반려된 뒤 다시 신청한 직원이 있는지 분석.
     */
    public String getAgencyRejectedThenReappliedAlert(Long memberNo) {
        validateExactRole(memberNo, "에이전시 관리자");

        Member admin = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Long agencyNo = admin.getAgency().getAgencyNo();

        List<AttendanceRequest> allRequests = attendanceRequestRepository.findByAgencyNoWithMember(agencyNo);
        Map<Long, List<AttendanceRequest>> byMember = allRequests.stream()
            .collect(Collectors.groupingBy(ar -> ar.getMember().getMemberNo()));

        List<String> reappliedLines = new ArrayList<>();
        for (Map.Entry<Long, List<AttendanceRequest>> entry : byMember.entrySet()) {
            List<AttendanceRequest> list = entry.getValue().stream()
                .sorted((a, b) -> (a.getAttendanceRequestCreatedAt() != null && b.getAttendanceRequestCreatedAt() != null)
                    ? a.getAttendanceRequestCreatedAt().compareTo(b.getAttendanceRequestCreatedAt()) : 0)
                .toList();
            LocalDateTime rejectedAt = null;
            boolean hasRequestAfterRejected = false;
            String latestStatus = null;
            String latestType = null;
            for (AttendanceRequest ar : list) {
                if (rejectedAt == null && "REJECTED".equals(ar.getAttendanceRequestStatus()) && ar.getAttendanceRequestCreatedAt() != null) {
                    rejectedAt = ar.getAttendanceRequestCreatedAt();
                }
                if (rejectedAt != null && ar.getAttendanceRequestCreatedAt() != null && ar.getAttendanceRequestCreatedAt().isAfter(rejectedAt)) {
                    hasRequestAfterRejected = true;
                    latestStatus = ar.getAttendanceRequestStatus();
                    latestType = ar.getAttendanceRequestType();
                }
            }
            if (rejectedAt != null && hasRequestAfterRejected) {
                String memberName = list.get(0).getMember().getMemberName() + "(" + list.get(0).getMember().getMemberRole() + ")";
                reappliedLines.add(String.format("%s: 반려 이력 후 재신청 있음 (최근: %s, %s)", memberName,
                    latestType != null ? latestType : "-", latestStatus != null ? latestStatus : "-"));
            }
        }

        String listText = reappliedLines.isEmpty()
            ? "해당 직원 없음"
            : String.join("\n", reappliedLines);

        String templateText =
            "너는 웹툰/웹소설 에이전시의 경영진을 돕는 AI 어시스턴트야. " +
            "휴가·근태 신청이 한 번이라도 반려된 뒤, 다시 근태를 신청한 직원 목록이야:\n\n{listText}\n\n" +
            "이 정보를 바탕으로, 해당 직원들이 재신청 중이거나 반려 사유를 수정해 다시 신청했을 수 있음을 짧게 안내해 줘. " +
            "검토·배려가 필요할 수 있다는 점을 2~4문장으로 요약해 줘. 해당 직원이 없으면 그대로 '해당 직원 없음'이라고 요약해 줘.";

        return callAi(templateText, Map.of("listText", listText));
    }

    public String getArtistLeaveRecommendation(Long memberNo) {
        validateArtistRole(memberNo);

        LeaveBalance balance = leaveBalanceRepository
            .findByMember_MemberNoAndLeaveBalanceYear(memberNo, String.valueOf(LocalDate.now().getYear()))
            .orElse(null);
        if (balance == null) {
            return "올해 연차 정보가 아직 등록되지 않았습니다. 에이전시 관리자에게 문의해 주세요.";
        }

        int total = balance.getLeaveBalanceTotalDays();
        double remain = balance.getLeaveBalanceRemainDays() != null ? balance.getLeaveBalanceRemainDays() : total;
        double used = total - remain;
        double usageRate = total > 0 ? (used / total) * 100 : 0;
        int currentMonth = LocalDate.now().getMonthValue();
        int expectedRate = getExpectedUsageRateForMonth(currentMonth);

        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusDays(60);
        List<KanbanCard> upcomingCards = kanbanCardRepository
            .findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(memberNo, "N");
        List<KanbanCard> nearCards = upcomingCards.stream()
            .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(today) && !c.getKanbanCardEndedAt().isAfter(rangeEnd))
            .toList();

        StringBuilder taskInfo = new StringBuilder();
        if (nearCards.isEmpty()) {
            taskInfo.append("향후 60일간 마감 예정 업무: 없음\n");
        } else {
            taskInfo.append(String.format("향후 60일간 마감 예정 업무 %d건:\n", nearCards.size()));
            for (KanbanCard card : nearCards) {
                taskInfo.append(String.format("- [%s] %s (마감: %s)\n",
                    card.getKanbanBoard().getProject().getProjectName(),
                    card.getKanbanCardName(),
                    card.getKanbanCardEndedAt()));
            }
        }

        String templateText =
            "너는 마감지기를 사용하는 웹툰/웹소설 작가의 워라밸을 챙겨주는 따뜻한 매니저야.\n\n" +
            "작가의 연차 현황:\n" +
            "- 총 연차: {total}일, 사용: {used}일, 잔여: {remain}일\n" +
            "- 현재 소진율: {usageRate}% (현재 {month}월 권장 소진율: {expectedRate}%)\n\n" +
            "작가의 업무 일정:\n{taskInfo}\n\n" +
            "오늘 날짜: {today}. (이미 지난 날짜는 절대 추천하지 말 것. 오늘 이후 날짜만 추천할 것.)\n\n" +
            "위 정보를 바탕으로:\n" +
            "1) 현재 소진율이 권장 대비 적절한지 평가해 줘\n" +
            "2) 소진율이 부족할 때만, 업무 마감일 사이의 여유 구간을 찾아 오늘 이후의 구체적인 연차 사용 추천일을 제안해 줘. " +
            "소진율이 이미 적절하면 연차를 언제 쓸지 추천하는 문장은 넣지 말고, 적절하다는 평가만 해 줘\n" +
            "3) 번아웃 예방 관점에서 조언을 덧붙여 줘\n\n" +
            "친근하고 부드러운 말투로 5~7문장 이내로 작성해. 의학적 진단은 배제해.";

        return callAi(templateText, Map.of(
            "total", total,
            "used", String.format("%.1f", used),
            "remain", String.format("%.1f", remain),
            "usageRate", String.format("%.1f", usageRate),
            "month", currentMonth,
            "expectedRate", expectedRate,
            "taskInfo", taskInfo.toString(),
            "today", today.toString()
        ));
    }

    public String getManagerLeaveRecommendation(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        Manager manager = managerRepository.findByMember_MemberNo(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자 정보를 찾을 수 없습니다."));
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());

        if (assignments.isEmpty()) {
            return "현재 배정된 작가가 없습니다.";
        }

        int currentMonth = LocalDate.now().getMonthValue();
        int expectedRate = getExpectedUsageRateForMonth(currentMonth);
        String currentYear = String.valueOf(LocalDate.now().getYear());
        LocalDate today = LocalDate.now();

        StringBuilder artistData = new StringBuilder();
        int belowExpected = 0;

        for (ArtistAssignment assignment : assignments) {
            Member artist = assignment.getArtist();
            Long mNo = artist.getMemberNo();

            LeaveBalance balance = leaveBalanceRepository
                .findByMember_MemberNoAndLeaveBalanceYear(mNo, currentYear)
                .orElse(null);

            int total = 0;
            double remain = 0, used = 0, rate = 0;
            if (balance != null) {
                total = balance.getLeaveBalanceTotalDays();
                remain = balance.getLeaveBalanceRemainDays() != null ? balance.getLeaveBalanceRemainDays() : total;
                used = total - remain;
                rate = total > 0 ? (used / total) * 100 : 0;
            }

            long pendingTasks = kanbanCardRepository.countByProjectMember_Member_MemberNo(mNo);
            String status = rate < expectedRate ? "소진 부족" : "적정";
            if (rate < expectedRate) belowExpected++;

            artistData.append(String.format("- %s(%s): 연차 %d일 중 %.1f일 사용(소진율 %.1f%%), 미완료 업무 %d건, 상태: %s\n",
                artist.getMemberName(), artist.getMemberRole(),
                total, used, rate, pendingTasks, status));
        }

        String templateText =
            "너는 웹툰/웹소설 에이전시의 담당자를 돕는 AI 어시스턴트야.\n\n" +
            "현재 {month}월 기준 권장 연차 소진율: {expectedRate}%\n" +
            "담당 작가 {count}명 중 소진 부족: {belowCount}명\n\n" +
            "각 작가 현황:\n{artistData}\n" +
            "위 정보를 바탕으로:\n" +
            "1) 소진율이 부족한 작가들에게 연차 사용을 독려하는 관리 조언을 해 줘\n" +
            "2) 업무량(미완료 업무 수)을 고려하여 현실적인 연차 권유 방법을 제안해 줘\n" +
            "3) 조직 차원에서 번아웃 예방을 위한 팁을 덧붙여 줘\n\n" +
            "3~5문장으로 작성해. 의학적 진단은 배제해.";

        return callAi(templateText, Map.of(
            "month", currentMonth,
            "expectedRate", expectedRate,
            "count", assignments.size(),
            "belowCount", belowExpected,
            "artistData", artistData.toString()
        ));
    }

    public String getManagerArtistWorkloadBalance(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        Manager manager = managerRepository.findByMember_MemberNo(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자 정보를 찾을 수 없습니다."));
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());

        if (assignments.isEmpty()) {
            return "현재 배정된 작가가 없습니다.";
        }

        LocalDate today = LocalDate.now();
        LocalDate in7 = today.plusDays(7);
        StringBuilder artistData = new StringBuilder();

        for (ArtistAssignment assignment : assignments) {
            Member artist = assignment.getArtist();
            Long mNo = artist.getMemberNo();

            long pastDueY = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(mNo, today, "Y");
            long pastDueN = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(mNo, today, "N");
            long pastDueTotal = pastDueY + pastDueN;
            double compliance = pastDueTotal > 0 ? (pastDueY * 100.0 / pastDueTotal) : 100.0;

            long incomplete = kanbanCardRepository.countByProjectMember_Member_MemberNo(mNo);
            List<KanbanCard> incompleteList =
                kanbanCardRepository.findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(mNo, "N");
            long in7Days = incompleteList.stream()
                .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(today) && !c.getKanbanCardEndedAt().isAfter(in7))
                .count();

            long completed = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardStatus(mNo, "Y");

            artistData.append(String.format("- %s(%s): 마감 준수율 %.0f%%, 미완료 %d건, 7일 내 마감 %d건, 완료 누적 %d건\n",
                artist.getMemberName(), artist.getMemberRole(), compliance, incomplete, in7Days, completed));
        }

        String templateText =
            "너는 웹툰/웹소설 에이전시의 담당자를 돕는 AI 어시스턴트야.\n\n" +
            "담당 작가별 업무 현황이야:\n{artistData}\n" +
            "위를 바탕으로 '누가 상대적으로 과부하인지', '업무 분배를 조정할 여지가 있는지'를 " +
            "2~3문장으로 요약해 줘. 친근한 말투로.";

        return callAi(templateText, Map.of("artistData", artistData.toString()));
    }

    public String getManagerMyHealthFeedback(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        int phq9 = getLatestScoreByOrderRange(memberNo, "월간 정신", 1, 9);
        int gad = getLatestScoreByOrderRange(memberNo, "월간 정신", 10, 19);
        int dash = getLatestTotalScore(memberNo, "월간 신체");

        String templateText =
            "너는 마감지기를 사용하는 웹툰/웹소설 에이전시 담당자의 건강을 챙겨주는 따뜻한 매니저야. " +
            "담당자의 최근 건강 설문 결과를 3가지 검사별로 알려줄게.\n" +
            "1) 우울 지수(PHQ-9): {phq9}점 (27점 만점, 10점 이상이면 주의)\n" +
            "2) 불안 지수(GAD): {gad}점 (40점 만점, 16점 이상이면 주의)\n" +
            "3) 손목/어깨 통증 지수(QuickDASH): {dash}점 (55점 만점, 25점 이상이면 주의)\n\n" +
            "각 검사 결과를 개별적으로 분석해서, " +
            "의학적 진단은 절대 배제하고, 현재 점수 상태에 대한 공감과 함께 " +
            "일상이나 업무 중에 할 수 있는 가벼운 스트레칭, 마인드컨트롤 팁을 " +
            "친근하고 부드러운 말투로 조언해 줘. " +
            "각 검사별 1~2문장, 총 5문장 이내로 작성해.";

        return callAi(templateText, Map.of("phq9", phq9, "gad", gad, "dash", dash));
    }

    public String getManagerWorkationRecommendation(Long memberNo) {
        validateExactRole(memberNo, "담당자");
        return buildWorkationRecommendation(memberNo, "담당자");
    }

    public String getManagerNudgeMessageRecommendation(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        Manager manager = managerRepository.findByMember_MemberNo(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자 정보를 찾을 수 없습니다."));
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());

        if (assignments.isEmpty()) {
            return "현재 배정된 작가가 없습니다.";
        }

        LocalDate today = LocalDate.now();
        List<String> lowComplianceArtists = new java.util.ArrayList<>();

        for (ArtistAssignment assignment : assignments) {
            Member artist = assignment.getArtist();
            Long mNo = artist.getMemberNo();
            long pastDueY = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(mNo, today, "Y");
            long pastDueN = kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardEndedAtBeforeAndKanbanCardStatus(mNo, today, "N");
            long pastDueTotal = pastDueY + pastDueN;
            double compliance = pastDueTotal > 0 ? (pastDueY * 100.0 / pastDueTotal) : 100.0;
            if (pastDueTotal >= 1 && compliance < 80.0) {
                lowComplianceArtists.add(String.format("%s(%s): 마감 준수율 %.0f%%", artist.getMemberName(), artist.getMemberRole(), compliance));
            }
        }

        if (lowComplianceArtists.isEmpty()) {
            return "마감 준수율이 낮은 배정 작가가 없어요. 현재 모두 80% 이상 준수하고 있어요.";
        }

        String artistList = String.join("\n", lowComplianceArtists);
        String templateText =
            "너는 웹툰/웹소설 에이전시의 담당자를 돕는 AI 어시스턴트야.\n\n" +
            "아래 작가들은 마감 준수율이 80% 미만이야. 이들에게 업무 독촉을 할 때 쓰기 좋은 메시지를 추천해 줘.\n\n" +
            "{artistList}\n\n" +
            "규칙: 최대한 기분이 안 나쁘고, 둥글고 부드럽게 전달할 수 있는 말로 각 작가별 1~2문장씩 추천해 줘. " +
            "비난이나 강압 톤 없이, 격려와 이해를 담은 독촉 문구로 작성해 줘.";

        return callAi(templateText, Map.of("artistList", artistList));
    }

    public String getManagerArtistDailyHealthSummary(Long memberNo) {
        validateExactRole(memberNo, "담당자");

        Manager manager = managerRepository.findByMember_MemberNo(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자 정보를 찾을 수 없습니다."));
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());

        if (assignments.isEmpty()) {
            return "현재 배정된 작가가 없습니다.";
        }

        StringBuilder summary = new StringBuilder();
        for (ArtistAssignment assignment : assignments) {
            Member artist = assignment.getArtist();
            Long mNo = artist.getMemberNo();
            List<DailyHealthCheck> recent = dailyHealthCheckRepository.findTop30ByMember_MemberNoOrderByHealthCheckCreatedAtDesc(mNo);
            if (recent.isEmpty()) {
                summary.append(String.format("- %s(%s): 일일 건강 체크 기록 없음\n", artist.getMemberName(), artist.getMemberRole()));
                continue;
            }

            int maxConsecutiveBad = 0;
            int currentBad = 0;
            double sleepSum = 0;
            int sleepCount = 0;
            int sleepLowDays = 0;
            int discomfortHighCount = 0;
            for (DailyHealthCheck d : recent) {
                boolean goodCondition = "좋음".equals(d.getHealthCondition()) || "최상".equals(d.getHealthCondition());
                if (!goodCondition && d.getHealthCondition() != null) {
                    currentBad++;
                    maxConsecutiveBad = Math.max(maxConsecutiveBad, currentBad);
                } else {
                    currentBad = 0;
                }
                if (d.getSleepHours() != null) {
                    sleepSum += d.getSleepHours();
                    sleepCount++;
                    if (d.getSleepHours() <= 4) sleepLowDays++;
                }
                if (d.getDiscomfortLevel() != null && d.getDiscomfortLevel() >= 8) discomfortHighCount++;
            }
            double avgSleep = sleepCount > 0 ? sleepSum / sleepCount : 0;

            summary.append(String.format("- %s(%s): 최근 %d회 기록, 컨디션 연속 나쁨 최대 %d일, 평균 수면 %.1f시간(4h 이하 %d회), 불편도 8 이상 %d회\n",
                artist.getMemberName(), artist.getMemberRole(), recent.size(), maxConsecutiveBad, avgSleep, sleepLowDays, discomfortHighCount));
        }

        String templateText =
            "너는 웹툰/웹소설 에이전시의 담당자를 돕는 AI 어시스턴트야.\n\n" +
            "배정 작가들의 일일 건강 체크(출근 시 컨디션·수면·불편도) 요약이야:\n{summary}\n" +
            "위 데이터를 바탕으로: 컨디션이 연속으로 안 좋은 작가, 평균 수면 4시간 이하로 낮은 작가, 불편도가 높았던 적이 있는 작가 등을 짚어 주고, " +
            "담당자가 챙겨볼 만한 포인트를 3~5문장으로 요약해 줘. 의학적 진단은 배제하고, 관리·배려 관점으로 부드럽게.";

        return callAi(templateText, Map.of("summary", summary.toString()));
    }

    public String getArtistWorkloadSummary(Long memberNo) {
        validateArtistRole(memberNo);

        List<KanbanCard> incomplete =
            kanbanCardRepository.findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(memberNo, "N");
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<KanbanCard> weekCards = incomplete.stream()
            .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(weekStart) && !c.getKanbanCardEndedAt().isAfter(weekEnd))
            .toList();
        List<KanbanCard> monthCards = incomplete.stream()
            .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(today) && !c.getKanbanCardEndedAt().isAfter(monthEnd))
            .toList();

        Map<LocalDate, Long> weekByDay = weekCards.stream()
            .collect(Collectors.groupingBy(KanbanCard::getKanbanCardEndedAt, Collectors.counting()));
        Map<LocalDate, Long> monthByDay = monthCards.stream()
            .collect(Collectors.groupingBy(KanbanCard::getKanbanCardEndedAt, Collectors.counting()));

        String weekDist = weekStart + "~" + weekEnd + " 요일별: " +
            weekByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey().getDayOfWeek() + " " + e.getValue() + "건")
                .collect(Collectors.joining(", "));
        if (weekByDay.isEmpty()) weekDist = weekStart + "~" + weekEnd + " 마감 예정 없음";

        long monthTotal = monthCards.size();
        String busiest = monthByDay.isEmpty() ? "없음" :
            monthByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + "건)")
                .orElse("없음");

        String templateText =
            "너는 마감지기를 사용하는 웹툰/웹소설 작가의 업무 관리를 돕는 AI 어시스턴트야.\n\n" +
            "작가의 이번 주·이번 달 마감 업무 현황이야.\n" +
            "- 이번 주(월~일) 마감 분포: {weekDist}\n" +
            "- 이번 달(오늘~말일) 마감 총 건수: {monthTotal}건\n" +
            "- 이번 달 가장 바쁜 날: {busiest}\n\n" +
            "위 데이터를 바탕으로 '이번 주 전반적으로 어떤지', '몇 일 전후가 가장 바쁜지'를 " +
            "친근한 말투로 3~4문장 요약해 줘. 격려 한 마디도 넣어 줘.";

        return callAi(templateText, Map.of(
            "weekDist", weekDist,
            "monthTotal", monthTotal,
            "busiest", busiest
        ));
    }

    public String getArtistProjectPriorityAdvice(Long memberNo) {
        validateArtistRole(memberNo);

        List<KanbanCard> incomplete =
            kanbanCardRepository.findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(memberNo, "N");
        LocalDate today = LocalDate.now();
        LocalDate in7 = today.plusDays(7);

        Map<String, List<KanbanCard>> byProject = incomplete.stream()
            .collect(Collectors.groupingBy(c -> c.getKanbanBoard().getProject().getProjectName()));

        StringBuilder projectData = new StringBuilder();
        for (Map.Entry<String, List<KanbanCard>> e : byProject.entrySet()) {
            String name = e.getKey();
            List<KanbanCard> cards = e.getValue();
            long in7Days = cards.stream()
                .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(today) && !c.getKanbanCardEndedAt().isAfter(in7))
                .count();
            String nearest = cards.stream()
                .map(KanbanCard::getKanbanCardEndedAt)
                .filter(d -> d != null)
                .filter(d -> !d.isBefore(today))
                .min(LocalDate::compareTo)
                .map(this::formatDeadlineForPrompt)
                .orElse("없음");
            projectData.append(String.format("- %s: 미완료 %d건, 7일 내 마감 %d건, 가장 가까운 마감 %s\n", name, cards.size(), in7Days, nearest));
        }

        if (byProject.isEmpty()) {
            return "현재 배정된 미완료 업무가 없어요. 담당자에게 할 일을 요청해 보세요.";
        }

        String templateText =
            "너는 웹툰/웹소설 작가의 업무 우선순위를 돕는 AI 어시스턴트야.\n\n" +
            "참여 중인 프로젝트별 현황이야:\n{projectData}\n\n" +
            "규칙: 마감일을 말할 때 반드시 전달된 그대로 '4월 15일', '5월 말'처럼 구체적인 월·일을 사용할 것. " +
            "'2026년으로 여유 있다'처럼 연도만 퉁쳐서 말하지 말 것.\n\n" +
            "위를 바탕으로 '지금은 어떤 프로젝트에 더 집중할지', '어떤 건 다음 주에 몰아서 해도 되는지' 같은 " +
            "우선순위·타이밍 조언을 3~4문장으로 친근하게 해 줘.";

        return callAi(templateText, Map.of("projectData", projectData.toString()));
    }

    public String getArtistWorkationRecommendation(Long memberNo) {
        validateArtistRole(memberNo);
        return buildWorkationRecommendation(memberNo, "작가");
    }

    private String buildWorkationRecommendation(Long memberNo, String roleLabel) {
        List<KanbanCard> incomplete =
            kanbanCardRepository.findByProjectMember_Member_MemberNoAndKanbanCardStatusOrderByKanbanCardEndedAtAsc(memberNo, "N");
        LocalDate today = LocalDate.now();
        LocalDate in14 = today.plusDays(14);

        List<KanbanCard> next14 = incomplete.stream()
            .filter(c -> c.getKanbanCardEndedAt() != null && !c.getKanbanCardEndedAt().isBefore(today) && !c.getKanbanCardEndedAt().isAfter(in14))
            .toList();

        Map<LocalDate, Long> byDay = next14.stream()
            .collect(Collectors.groupingBy(KanbanCard::getKanbanCardEndedAt, Collectors.counting()));
        String workloadDesc = byDay.isEmpty()
            ? "향후 2주 마감 예정 없음 (한가한 편)"
            : "향후 2주 일별 마감: " + byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " " + e.getValue() + "건")
                .collect(Collectors.joining(", "));

        List<LocalDate> lightDays = byDay.entrySet().stream()
            .filter(e -> e.getValue() <= 1)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        String lightDesc = lightDays.isEmpty() ? "모든 날 마감이 2건 이상이라 바쁜 편" : "상대적으로 한가한 날: " + lightDays.stream().map(LocalDate::toString).collect(Collectors.joining(", "));

        String templateText =
            "너는 마감지기를 사용하는 웹툰/웹소설 " + roleLabel + "의 워라밸을 챙기는 AI 어시스턴트야.\n\n" +
            roleLabel + "의 향후 2주 업무 밀도:\n{workloadDesc}\n{lightDesc}\n\n" +
            "1) 위 한가한 구간을 참고해 워케이션 계획하기 좋은 시기를 1~2문장으로 추천해 줘.\n" +
            "2) 워케이션 추천 장소 3곳을 골라 간단히 소개해 줘. (지역, 분위기, 작업하기 좋은 이유 한 줄씩, 실제 존재하는 장소만.)\n" +
            "   - 반드시 매번 다른 조합으로 추천해 줘. 제주·강릉·부산처럼 자주 쓰는 후보만 반복하지 말고, 국내는 소규모 도시·섬·산·해변·동네 등 다양한 지역을 골라 줘.\n" +
            "   - 3곳 중 최소 1곳은 해외 휴양지(동남아·일본·대만·유럽 등)로 골라 줘.\n\n" +
            "친근하고 부드러운 말투로 5~6문장 이내로 작성해.";

        return callAi(templateText, Map.of(
            "workloadDesc", workloadDesc,
            "lightDesc", lightDesc
        ));
    }

    // === private helpers ===

    private boolean isArtistRole(String role) {
        return ARTIST_KEYWORDS.stream().anyMatch(role::contains);
    }

    private void validateArtistRole(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
        if (!isArtistRole(member.getMemberRole().trim())) {
            throw new SecurityException("아티스트 전용 API입니다. (현재 역할: " + member.getMemberRole() + ")");
        }
    }

    private void validateExactRole(Long memberNo, String expectedRole) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
        if (!expectedRole.equals(member.getMemberRole().trim())) {
            throw new SecurityException("해당 API에 접근 권한이 없습니다. (현재 역할: " + member.getMemberRole() + ")");
        }
    }

    private String evaluateRiskPhq9(int score) {
        if (score <= 4) return "정상";
        if (score <= 9) return "경미";
        if (score <= 14) return "주의";
        if (score <= 19) return "경고";
        return "위험";
    }

    private String evaluateRiskGad(int score) {
        if (score <= 7) return "정상";
        if (score <= 15) return "경미";
        if (score <= 24) return "주의";
        if (score <= 32) return "경고";
        return "위험";
    }

    private String evaluateRiskDash(int score) {
        if (score <= 15) return "정상";
        if (score <= 24) return "경미";
        if (score <= 34) return "주의";
        if (score <= 44) return "경고";
        return "위험";
    }

    /** AI/사용자에게 전달할 때 마감일을 '4월 15일' 형태로 표기 (연도 생략, 애매한 '2026년' 방지) */
    private String formatDeadlineForPrompt(LocalDate date) {
        if (date == null) return "없음";
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return month + "월 " + day + "일";
    }

    private int getExpectedUsageRateForMonth(int month) {
        return switch (month) {
            case 1 -> 5;   case 2 -> 8;   case 3 -> 10;
            case 4 -> 12;  case 5 -> 15;  case 6 -> 20;
            case 7 -> 25;  case 8 -> 30;  case 9 -> 40;
            case 10 -> 50; case 11 -> 85; case 12 -> 95;
            default -> 0;
        };
    }

    private String callAi(String templateText, Map<String, Object> variables) {
        String tone = ToneContext.get();
        if (tone != null && !tone.isEmpty()) {
            String instruction = getToneInstruction(tone);
            if (instruction != null && !instruction.isEmpty()) {
                templateText = templateText + "\n\n[말투 지시] " + instruction;
            }
        }
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        Prompt prompt = promptTemplate.create(variables);
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    /**
     * 챗봇 말투 키에 따른 지시문. 프론트와 동일한 키 사용 (standard, romance_villainess, noble_male, cyworld, sageuk).
     */
    private String getToneInstruction(String toneKey) {
        if (toneKey == null) return null;
        return switch (toneKey) {
            case "romance_villainess" -> "답변 전체를 로맨스 판타지 악역 영애 말투로 작성해 줘. (예: '이런 건 당연히 알고 있어야 하는 거 아니야?', '흥, 그 정도는 내가 봐줄게.' 같은 느낌, 하지만 도움 되는 내용은 유지해 줘.)";
            case "noble_male" -> "답변 전체를 귀한 신분의 귀족 남자 말투로 작성해 줘. (예: '그대의 안녕을 바라노라.', '허락하겠다.' 같은 존댓말·고풍스러운 표현, 하지만 도움 되는 내용은 유지해 줘.)";
            case "cyworld" -> "답변 전체를 2000년대 싸이월드·초딩 감성 말투로 작성해 줘. (예: 'ㅋㅋㅋ', '~해여', '뿌우~', '오늘도 파이팅!', 이모티콘 느낌, 하지만 도움 되는 내용은 유지해 줘.)";
            case "sageuk" -> "답변 전체를 사극·무림 말투로 작성해 줘. (예: '소인 알겠나.', '그대에게 한 수 가르쳐 주겠다.', '천천히 하시게.' 같은 느낌, 하지만 도움 되는 내용은 유지해 줘.)";
            case "butler" -> "답변 전체를 집사 말투로 작성해 줘. 주인님·아가님으로 상대를 부르고, '~하시옵소서', '모시겠사옵나이다', '알겠사옵나이다'처럼 정중하면서도 자연스러운 고어체 존댓말을 써 줘. 딱딱하지 않게 배려하는 느낌을 유지하고, 도움 되는 내용은 그대로 전달해 줘.";
            case "maid" -> "답변 전체를 메이드 말투로 작성해 줘. 주인님·아가님으로 상대를 부르고, '저, ~해 드릴게요', '주인님께서는~', '아가님 편히 쉬시도록'처럼 공손하고 다정하면서 자연스러운 말투로. 과하지 않게 주인님·아가님 호칭을 살리고, 도움 되는 내용은 그대로 전달해 줘.";
            case "standard" -> null;
            default -> null;
        };
    }

    private int getLatestTotalScore(Long memberNo, String healthSurveyQuestionType) {
        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository
            .findByMemberNoAndHealthSurveyType(memberNo, healthSurveyQuestionType);
        return sumLatestScores(items);
    }

    private int getLatestScoreByOrderRange(Long memberNo, String type, int minOrder, int maxOrder) {
        List<HealthSurveyResponseItem> items = healthSurveyResponseItemRepository
            .findByMemberNoAndTypeAndOrderRange(memberNo, type, minOrder, maxOrder);
        return sumLatestScores(items);
    }

    private int sumLatestScores(List<HealthSurveyResponseItem> items) {
        if (items == null || items.isEmpty()) return 0;

        Map<LocalDateTime, List<HealthSurveyResponseItem>> byCreatedAt = items.stream()
            .filter(i -> i.getHealthSurveyQuestionItemCreatedAt() != null)
            .collect(Collectors.groupingBy(HealthSurveyResponseItem::getHealthSurveyQuestionItemCreatedAt));
        LocalDateTime latest = byCreatedAt.keySet().stream()
            .max(LocalDateTime::compareTo)
            .orElse(null);
        if (latest == null) return 0;

        return byCreatedAt.get(latest).stream()
            .mapToInt(i -> i.getHealthSurveyQuestionItemAnswerScore() != null
                ? i.getHealthSurveyQuestionItemAnswerScore() : 0)
            .sum();
    }
}
