package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.attendance.entity.ProjectLeaveRequest;
import com.kh.magamGG.domain.attendance.repository.AttendanceRepository;
import com.kh.magamGG.domain.attendance.repository.ProjectLeaveRequestRepository;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.AssignableManagerResponse;
import com.kh.magamGG.domain.project.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.dto.response.NextSerialProjectItemResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final KanbanCardRepository kanbanCardRepository;
    private final ProjectLeaveRequestRepository projectLeaveRequestRepository;
    private final AttendanceRepository attendanceRepository;

    private static final int DEADLINE_WARNING_DAYS = 7;
    private static final int PROGRESS_WARNING_THRESHOLD = 70;

    @Override
    public List<ManagedProjectResponse> getManagedProjectsByManager(Long memberNo) {
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

        List<ProjectMember> allProjectMembers = new ArrayList<>();
        for (Long artistNo : artistMemberNos) {
            allProjectMembers.addAll(projectMemberRepository.findByMember_MemberNo(artistNo));
        }

        Set<Long> seenProjectNos = new HashSet<>();
        List<ManagedProjectResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 정렬을 위해 nextSerialDate를 함께 저장할 임시 클래스
        class ProjectWithDeadline {
            ManagedProjectResponse response;
            LocalDate nextSerialDate;
            
            ProjectWithDeadline(ManagedProjectResponse response, LocalDate nextSerialDate) {
                this.response = response;
                this.nextSerialDate = nextSerialDate;
            }
        }
        
        List<ProjectWithDeadline> tempList = new ArrayList<>();

        for (ProjectMember pm : allProjectMembers) {
            var project = pm.getProject();
            if (seenProjectNos.contains(project.getProjectNo())) {
                continue;
            }
            seenProjectNos.add(project.getProjectNo());

            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            int total = cards.size();
            int completed = (int) cards.stream()
                    .filter(c -> "Y".equals(c.getKanbanCardStatus()))
                    .count();
            int progress = total > 0 ? (completed * 100 / total) : 0;

            // 주기 기준 다음 연재일 사용 (projectStartedAt + n * projectCycle)
            LocalDate nextSerialDate = computeNextSerialDate(project, today);
            String deadlineStr = nextSerialDate != null
                    ? (nextSerialDate.getMonthValue() + "월 " + nextSerialDate.getDayOfMonth() + "일")
                    : "-";
            long daysUntilDeadline = nextSerialDate != null
                    ? ChronoUnit.DAYS.between(today, nextSerialDate)
                    : 999L;
            boolean isDeadlineSoon = daysUntilDeadline >= 0 && daysUntilDeadline <= DEADLINE_WARNING_DAYS;
            String status = (isDeadlineSoon && progress < PROGRESS_WARNING_THRESHOLD) ? "주의" : "정상";

            String artistName = pm.getMember().getMemberName();

            ManagedProjectResponse response = ManagedProjectResponse.builder()
                    .projectNo(project.getProjectNo())
                    .projectName(project.getProjectName())
                    .artist(artistName)
                    .status(status)
                    .progress(progress)
                    .deadline(deadlineStr)
                    .build();
            
            tempList.add(new ProjectWithDeadline(response, nextSerialDate));
        }

        // 마감일(다음 연재일) 기준으로 정렬: 오늘이 연재일인 항목이 제일 위, 그 다음 가까운 순서대로
        tempList.sort((a, b) -> {
            if (a.nextSerialDate == null && b.nextSerialDate == null) {
                return Long.compare(a.response.getProjectNo(), b.response.getProjectNo());
            }
            if (a.nextSerialDate == null) return 1;  // null은 뒤로
            if (b.nextSerialDate == null) return -1; // null은 뒤로
            
            // 오늘이 연재일인 항목을 최우선으로
            boolean aIsToday = a.nextSerialDate.equals(today);
            boolean bIsToday = b.nextSerialDate.equals(today);
            if (aIsToday && !bIsToday) return -1;  // a가 오늘이면 앞으로
            if (!aIsToday && bIsToday) return 1;   // b가 오늘이면 앞으로
            
            // 둘 다 오늘이거나 둘 다 오늘이 아니면 날짜 오름차순
            return a.nextSerialDate.compareTo(b.nextSerialDate);
        });
        
        // 정렬된 결과를 result에 추가
        for (ProjectWithDeadline item : tempList) {
            result.add(item.response);
        }
        
        return result;
    }

    @Override
    public List<NextSerialProjectItemResponse> getNextSerialProjectsForMember(Long memberNo, int limit) {
        LocalDate today = LocalDate.now();
        List<ProjectMember> projectMembers = projectMemberRepository.findByMember_MemberNo(memberNo);
        Set<Long> seen = new HashSet<>();
        List<NextSerialProjectItemResponse> list = new ArrayList<>();
        for (ProjectMember pm : projectMembers) {
            Project p = pm.getProject();
            Long pno = p.getProjectNo();
            if (seen.contains(pno)) continue;
            seen.add(pno);

            LocalDateTime startedAt = p.getProjectStartedAt();
            if (startedAt == null) continue;

            // 승인된 휴재 기간을 스킵한 다음 연재일 계산
            LocalDate nextDate = computeNextSerialDateConsideringHiatus(p, today);
            if (nextDate == null) continue;

            list.add(NextSerialProjectItemResponse.builder()
                .projectNo(pno)
                .projectName(p.getProjectName())
                .projectColor(p.getProjectColor())
                .nextDeadline(nextDate.toString())
                .today(nextDate.equals(today))
                .build());
        }
        list.sort(Comparator.comparing(NextSerialProjectItemResponse::getNextDeadline));
        return list.size() <= limit ? list : list.subList(0, limit);
    }

    /**
     * 프로젝트 주기 기준 다음 연재일 계산. 승인된 휴재(ATTENDANCE_REQUEST_TYPE=휴재, STATUS=APPROVED) 기간은
     * 연재일에서 제외하고, 휴재 종료 이후의 다음 연재일을 반환한다.
     */
    private LocalDate computeNextSerialDateConsideringHiatus(Project p, LocalDate today) {
        LocalDateTime startedAt = p.getProjectStartedAt();
        if (startedAt == null) return null;
        LocalDate startDate = startedAt.toLocalDate();
        int cycleDays = (p.getProjectCycle() != null && p.getProjectCycle() > 0) ? p.getProjectCycle() : 7;

        List<ProjectLeaveRequest> approvedHiatus = projectLeaveRequestRepository.findApprovedHiatusByProjectNo(p.getProjectNo());
        List<LocalDate[]> hiatusPeriods = new ArrayList<>();
        for (ProjectLeaveRequest plr : approvedHiatus) {
            LocalDate hStart = plr.getAttendanceRequest().getAttendanceRequestStartDate().toLocalDate();
            LocalDate hEnd = plr.getAttendanceRequest().getAttendanceRequestEndDate().toLocalDate();
            hiatusPeriods.add(new LocalDate[] { hStart, hEnd });
        }

        // 연재일 후보: startDate + k * cycleDays (최대 약 2년치)
        int maxK = 365 * 2 / Math.max(1, cycleDays);
        for (int k = 0; k <= maxK; k++) {
            LocalDate candidate = startDate.plusDays((long) k * cycleDays);
            if (candidate.isBefore(today)) continue;
            boolean inHiatus = false;
            for (LocalDate[] period : hiatusPeriods) {
                if (!candidate.isBefore(period[0]) && !candidate.isAfter(period[1])) {
                    inHiatus = true;
                    break;
                }
            }
            if (!inHiatus) return candidate;
        }
        return null;
    }

    private static final String[] DAY_LABELS = {"오늘", "내일", "2일 후", "3일 후", "4일 후"};

    @Override
    public List<DeadlineCountResponse> getDeadlineCountsForManager(Long memberNo) {
        Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
        if (managerOpt.isEmpty()) {
            return buildEmptyDeadlineCounts();
        }
        Set<Long> artistMemberNos = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo())
                .stream()
                .map(a -> a.getArtist().getMemberNo())
                .collect(Collectors.toSet());
        if (artistMemberNos.isEmpty()) {
            return buildEmptyDeadlineCounts();
        }
        List<ProjectMember> allProjectMembers = new ArrayList<>();
        for (Long artistNo : artistMemberNos) {
            allProjectMembers.addAll(projectMemberRepository.findByMember_MemberNo(artistNo));
        }
        Set<Long> seenProjectNos = new HashSet<>();
        LocalDate today = LocalDate.now();
        int[] counts = new int[5];
        for (ProjectMember pm : allProjectMembers) {
            Project p = pm.getProject();
            if (seenProjectNos.contains(p.getProjectNo())) continue;
            seenProjectNos.add(p.getProjectNo());
            LocalDate nextDate = computeNextSerialDate(p, today);
            if (nextDate == null) continue;
            long daysDiff = ChronoUnit.DAYS.between(today, nextDate);
            if (daysDiff >= 0 && daysDiff <= 4) {
                counts[(int) daysDiff]++;
            }
        }
        return buildDeadlineCounts(counts);
    }

    /** 프로젝트 주기 기준 다음 연재일 (시작일 + n*주기) */
    private LocalDate computeNextSerialDate(Project p, LocalDate today) {
        if (p.getProjectStartedAt() == null) return null;
        LocalDate startDate = p.getProjectStartedAt().toLocalDate();
        int cycleDays = (p.getProjectCycle() != null && p.getProjectCycle() > 0) ? p.getProjectCycle() : 7;
        long daysBetween = ChronoUnit.DAYS.between(startDate, today);
        int n = daysBetween <= 0 ? 0 : (int) Math.ceil((double) daysBetween / cycleDays);
        return startDate.plusDays((long) n * cycleDays);
    }

    private List<DeadlineCountResponse> buildEmptyDeadlineCounts() {
        return Arrays.stream(DAY_LABELS)
                .map(label -> DeadlineCountResponse.builder().name(label).count(0).build())
                .collect(Collectors.toList());
    }

    private List<DeadlineCountResponse> buildDeadlineCounts(int[] counts) {
        List<DeadlineCountResponse> result = new ArrayList<>();
        for (int i = 0; i < DAY_LABELS.length; i++) {
            result.add(DeadlineCountResponse.builder()
                    .name(DAY_LABELS[i])
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    @Override
    public List<ProjectListResponse> getProjectsByMemberNo(Long memberNo) {
        List<ProjectMember> projectMembers = projectMemberRepository.findByMember_MemberNo(memberNo);
        Set<Long> seen = new HashSet<>();
        List<ProjectListResponse> result = new ArrayList<>();
        for (ProjectMember pm : projectMembers) {
            Long pno = pm.getProject().getProjectNo();
            if (seen.contains(pno)) continue;
            seen.add(pno);
            Project p = pm.getProject();
            List<ProjectMember> members = projectMemberRepository.findByProject_ProjectNo(pno);
            result.add(toProjectListResponse(p, members));
        }
        return result;
    }

    @Override
    public long getMyProjectCount(Long memberNo) {
        return projectMemberRepository.countDistinctProjectsByMemberNo(memberNo);
    }

    @Override
    public long getTaskCountByMemberNo(Long memberNo) {
        return kanbanCardRepository.countByProjectMember_Member_MemberNo(memberNo);
    }

    @Override
    public long getCompletedTaskCountByMemberNo(Long memberNo) {
        return kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardStatus(memberNo, "Y");
    }

    @Override
    public long getActiveTaskCountByMemberNo(Long memberNo) {
        return kanbanCardRepository.countByProjectMember_Member_MemberNoAndKanbanCardStatusNotD(memberNo);
    }

    @Override
    public List<ProjectListResponse> getProjectsByAgencyNo(Long agencyNo, Long requesterMemberNo) {
        Member requester = memberRepository.findById(requesterMemberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (requester.getAgency() == null || !requester.getAgency().getAgencyNo().equals(agencyNo)) {
            throw new IllegalArgumentException("해당 에이전시에 대한 접근 권한이 없습니다.");
        }
        if (!"에이전시 관리자".equals(requester.getMemberRole())) {
            throw new IllegalArgumentException("에이전시 관리자만 전체 프로젝트를 조회할 수 있습니다.");
        }
        List<Long> projectNos = projectMemberRepository.findDistinctProjectNosByMember_Agency_AgencyNo(agencyNo);
        List<ProjectListResponse> result = new ArrayList<>();
        for (Long pno : projectNos) {
            Project project = projectRepository.findById(pno).orElse(null);
            if (project == null) continue;
            List<ProjectMember> members = projectMemberRepository.findByProject_ProjectNo(pno);
            result.add(toProjectListResponse(project, members));
        }
        return result;
    }

    @Override
    @Transactional
    public ProjectListResponse createProject(ProjectCreateRequest request, Long creatorNo) {
        String projectName = request.getProjectName() != null ? request.getProjectName().trim() : "";
        if (projectName.isEmpty()) {
            throw new IllegalArgumentException("프로젝트명을 입력해주세요.");
        }

        Member artist = memberRepository.findById(request.getArtistMemberNo())
                .orElseThrow(() -> new IllegalArgumentException("작가 회원을 찾을 수 없습니다: " + request.getArtistMemberNo()));

        Optional<Manager> creatorManagerOpt = managerRepository.findByMember_MemberNo(creatorNo);
        if (creatorManagerOpt.isPresent()) {
            boolean assigned = artistAssignmentRepository.existsByManager_ManagerNoAndArtist_MemberNo(
                    creatorManagerOpt.get().getManagerNo(), request.getArtistMemberNo());
            if (!assigned) {
                throw new IllegalArgumentException("해당 담당자에게 배정된 작가만 프로젝트에 추가할 수 있습니다.");
            }
        }
        Long agencyNo = artist.getAgency() != null ? artist.getAgency().getAgencyNo() : null;
        if (agencyNo != null && projectRepository.countByProjectNameAndAgencyNo(projectName, agencyNo) > 0) {
            throw new IllegalArgumentException("이미 같은 이름의 프로젝트가 있습니다.");
        }

        Project project = new Project();
        project.setProjectName(projectName);
        project.setProjectStatus(request.getProjectStatus() != null ? request.getProjectStatus() : "연재");
        project.setProjectColor(request.getProjectColor() != null ? request.getProjectColor() : "기본색");
        project.setProjectCycle(request.getProjectCycle());
        project.setPlatform(request.getPlatform());
        project.setThumbnailFile(request.getThumbnailFile());
        project.setProjectStartedAt(request.getProjectStartedAt());
        Project saved = projectRepository.save(project);
        ProjectMember artistPm = new ProjectMember();
        artistPm.setMember(artist);
        artistPm.setProject(saved);
        artistPm.setProjectMemberRole("작가");
        projectMemberRepository.save(artistPm);

        Member creator = memberRepository.findById(creatorNo)
            .orElseThrow(() -> new IllegalArgumentException("생성자 회원을 찾을 수 없습니다: " + creatorNo));
        if (!creatorNo.equals(request.getArtistMemberNo())) {
            ProjectMember creatorPm = new ProjectMember();
            creatorPm.setMember(creator);
            creatorPm.setProject(saved);
            creatorPm.setProjectMemberRole("담당자");
            projectMemberRepository.save(creatorPm);
        }

        List<ProjectMember> members = projectMemberRepository.findByProject_ProjectNo(saved.getProjectNo());
        return toProjectListResponse(saved, members);
    }

    @Override
    public void ensureProjectAccess(Long memberNo, Long projectNo) {
        if (projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo)) {
            return;
        }
        // 에이전시 관리자: 소속 에이전시의 프로젝트면 조회 허용 (PROJECT_MEMBER가 아니어도 됨)
        Member requester = memberRepository.findById(memberNo).orElse(null);
        if (requester != null && "에이전시 관리자".equals(requester.getMemberRole())
            && requester.getAgency() != null) {
            Long agencyNo = requester.getAgency().getAgencyNo();
            List<ProjectMember> projectMembers = projectMemberRepository.findByProject_ProjectNo(projectNo);
            boolean projectBelongsToAgency = projectMembers.stream()
                .anyMatch(pm -> pm.getMember().getAgency() != null
                    && agencyNo.equals(pm.getMember().getAgency().getAgencyNo()));
            if (projectBelongsToAgency) {
                return;
            }
        }
        throw new IllegalArgumentException("해당 프로젝트에 대한 접근 권한이 없습니다.");
    }

    @Override
    public ProjectListResponse getProjectByNo(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        List<ProjectMember> members = projectMemberRepository.findByProject_ProjectNo(projectNo);
        return toProjectListResponse(project, members);
    }

    @Override
    @Transactional
    public ProjectListResponse updateProject(Long projectNo, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
        if (request.getProjectStatus() != null) project.setProjectStatus(request.getProjectStatus());
        if (request.getProjectColor() != null) project.setProjectColor(request.getProjectColor());
        if (request.getProjectCycle() != null) project.setProjectCycle(request.getProjectCycle());
        if (request.getPlatform() != null) project.setPlatform(request.getPlatform());
        if (request.getThumbnailFile() != null) project.setThumbnailFile(request.getThumbnailFile());
        if (request.getProjectStartedAt() != null) project.setProjectStartedAt(request.getProjectStartedAt());
        projectRepository.save(project);

        if (request.getArtistMemberNo() != null) {
            Long newArtistNo = request.getArtistMemberNo();
            Member newArtist = memberRepository.findById(newArtistNo)
                .orElseThrow(() -> new IllegalArgumentException("작가로 지정할 회원을 찾을 수 없습니다: " + newArtistNo));
            var optionalArtistRow = projectMemberRepository.findFirstByProject_ProjectNoAndProjectMemberRole(projectNo, "작가");
            if (optionalArtistRow.isPresent()) {
                ProjectMember artistPm = optionalArtistRow.get();
                if (!artistPm.getMember().getMemberNo().equals(newArtistNo)) {
                    artistPm.setMember(newArtist);
                    projectMemberRepository.save(artistPm);
                }
            } else {
                if (!projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, newArtistNo)) {
                    ProjectMember artistPm = new ProjectMember();
                    artistPm.setProject(project);
                    artistPm.setMember(newArtist);
                    artistPm.setProjectMemberRole("작가");
                    projectMemberRepository.save(artistPm);
                } else {
                    projectMemberRepository.findByProject_ProjectNo(projectNo).stream()
                        .filter(pm -> pm.getMember().getMemberNo().equals(newArtistNo))
                        .findFirst()
                        .ifPresent(pm -> {
                            pm.setProjectMemberRole("작가");
                            projectMemberRepository.save(pm);
                        });
                }
            }
        }

        List<ProjectMember> members = projectMemberRepository.findByProject_ProjectNo(projectNo);
        return toProjectListResponse(project, members);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectNo) {
        projectRepository.deleteById(projectNo);
    }

    @Override
    public List<ProjectMemberResponse> getProjectMembers(Long projectNo) {
        List<ProjectMember> list = projectMemberRepository.findByProject_ProjectNo(projectNo);
        LocalDate today = LocalDate.now();
        return list.stream().map(pm -> toProjectMemberResponse(pm, today)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addProjectMembers(Long projectNo, List<Long> memberNos) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        String projectName = project.getProjectName() != null ? project.getProjectName() : "프로젝트";
        for (Long memberNo : memberNos) {
            if (projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo)) continue;
            Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
            ProjectMember pm = new ProjectMember();
            pm.setMember(member);
            pm.setProject(project);
            pm.setProjectMemberRole("어시스트");
            projectMemberRepository.save(pm);
            // 추가된 멤버에게만 알림 저장 (NOTIFICATION_TYPE은 VARCHAR(10))
            try {
                notificationService.createNotification(
                    memberNo,
                    "프로젝트 추가",
                    projectName + " 프로젝트에 추가되었습니다.",
                    "PROJ_ADD"
                );
            } catch (Exception e) {
                log.error("팀원 추가 알림 저장 실패 memberNo={}, projectName={} - 원인: {}", memberNo, projectName, e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional
    public void removeProjectMember(Long projectNo, Long projectMemberNo) {
        ProjectMember pm = projectMemberRepository.findById(projectMemberNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트 멤버를 찾을 수 없습니다: " + projectMemberNo));
        if (!pm.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 멤버가 아닙니다.");
        }
        Long memberNo = pm.getMember().getMemberNo();
        String projectName = pm.getProject().getProjectName() != null ? pm.getProject().getProjectName() : "프로젝트";
        projectMemberRepository.delete(pm);
        // 제외된 멤버에게만 알림 저장 (NOTIFICATION_TYPE VARCHAR(10) 이하로)
        try {
            notificationService.createNotification(
                memberNo,
                "프로젝트 제외",
                projectName + " 프로젝트에서 제외되었습니다.",
                "PROJ_REM"
            );
        } catch (Exception e) {
            log.error("팀원 제외 알림 저장 실패 memberNo={}, projectName={} - 원인: {}", memberNo, projectName, e.getMessage(), e);
        }
    }

    @Override
    public List<ProjectMemberResponse> getAddableMembers(Long projectNo) {
        projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        List<ProjectMember> existing = projectMemberRepository.findByProject_ProjectNo(projectNo);
        if (existing.isEmpty()) return List.of();
        Long agencyNo = existing.get(0).getMember().getAgency().getAgencyNo();
        Set<Long> existingMemberNos = existing.stream().map(pm -> pm.getMember().getMemberNo()).collect(Collectors.toSet());
        List<String> excludeRoles = List.of("담당자", "웹툰 작가", "웹소설 작가", "에이전시 관리자");
        return memberRepository.findByAgency_AgencyNo(agencyNo).stream()
            .filter(m -> !existingMemberNos.contains(m.getMemberNo()))
            .filter(m -> !excludeRoles.contains(m.getMemberRole()))
            .map(m -> ProjectMemberResponse.builder()
                .memberNo(m.getMemberNo())
                .memberName(m.getMemberName())
                .memberEmail(m.getMemberEmail())
                .memberPhone(m.getMemberPhone())
                .memberRole(m.getMemberRole())
                .memberProfileImage(m.getMemberProfileImage())
                .memberStatus(m.getMemberStatus())
                .projectMemberRole(null)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<AssignableManagerResponse> getAssignableManagers(Long projectNo) {
        projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        List<ProjectMember> projectMembers = projectMemberRepository.findByProject_ProjectNo(projectNo);
        Set<Long> seenManagerNos = new HashSet<>();
        List<AssignableManagerResponse> result = new ArrayList<>();
        for (ProjectMember pm : projectMembers) {
            if ("담당자".equals(pm.getProjectMemberRole())) continue;
            Long artistMemberNo = pm.getMember().getMemberNo();
            artistAssignmentRepository.findByArtistMemberNo(artistMemberNo).ifPresent(assignment -> {
                Manager manager = assignment.getManager();
                if (manager != null && seenManagerNos.add(manager.getManagerNo())) {
                    Member managerMember = manager.getMember();
                    if (managerMember != null && "ACTIVE".equals(managerMember.getMemberStatus())) {
                        result.add(AssignableManagerResponse.builder()
                            .managerNo(manager.getManagerNo())
                            .memberNo(managerMember.getMemberNo())
                            .memberName(managerMember.getMemberName())
                            .build());
                    }
                }
            });
        }
        return result;
    }

    @Override
    @Transactional
    public void assignManagerToProject(Long projectNo, Long managerNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        Manager newManager = managerRepository.findById(managerNo)
            .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다: " + managerNo));
        Member newManagerMember = newManager.getMember();
        if (newManagerMember == null || !"ACTIVE".equals(newManagerMember.getMemberStatus())) {
            throw new IllegalArgumentException("활성 상태인 담당자만 배치할 수 있습니다.");
        }
        List<AssignableManagerResponse> assignable = getAssignableManagers(projectNo);
        boolean allowed = assignable.stream().anyMatch(a -> a.getManagerNo().equals(managerNo));
        if (!allowed) {
            throw new IllegalArgumentException("해당 담당자는 이 프로젝트 작가와 연결되어 있지 않아 배치할 수 없습니다.");
        }
        Optional<ProjectMember> currentManagerPm = projectMemberRepository.findFirstByProject_ProjectNoAndProjectMemberRole(projectNo, "담당자");
        ProjectMember newManagerPm = projectMemberRepository.findByProject_ProjectNo(projectNo).stream()
            .filter(pm -> pm.getMember().getMemberNo().equals(newManagerMember.getMemberNo()))
            .findFirst()
            .orElse(null);
        if (newManagerPm == null) {
            newManagerPm = new ProjectMember();
            newManagerPm.setProject(project);
            newManagerPm.setMember(newManagerMember);
            newManagerPm.setProjectMemberRole("담당자");
            newManagerPm = projectMemberRepository.save(newManagerPm);
        } else {
            newManagerPm.setProjectMemberRole("담당자");
            newManagerPm = projectMemberRepository.save(newManagerPm);
        }
        if (currentManagerPm.isPresent() && !currentManagerPm.get().getProjectMemberNo().equals(newManagerPm.getProjectMemberNo())) {
            ProjectMember oldPm = currentManagerPm.get();
            List<KanbanCard> cards = kanbanCardRepository.findByProjectMember_ProjectMemberNo(oldPm.getProjectMemberNo());
            for (KanbanCard card : cards) {
                card.setProjectMember(newManagerPm);
            }
            projectMemberRepository.delete(oldPm);
        }
    }

    private ProjectListResponse toProjectListResponse(Project project, List<ProjectMember> members) {
        String artistName = null;
        Long artistMemberNo = null;
        String managerName = null;
        Long managerMemberNo = null;
        for (ProjectMember pm : members) {
            if ("작가".equals(pm.getProjectMemberRole())) {
                artistName = pm.getMember().getMemberName();
                artistMemberNo = pm.getMember().getMemberNo();
            } else if ("담당자".equals(pm.getProjectMemberRole()) && managerName == null) {
                managerName = pm.getMember().getMemberName();
                managerMemberNo = pm.getMember().getMemberNo();
            }
        }
        return ProjectListResponse.builder()
            .projectNo(project.getProjectNo())
            .projectName(project.getProjectName())
            .projectStatus(project.getProjectStatus())
            .projectColor(project.getProjectColor())
            .thumbnailFile(project.getThumbnailFile())
            .artistName(artistName)
            .artistMemberNo(artistMemberNo)
            .projectGenre(null)
            .platform(project.getPlatform())
            .projectCycle(project.getProjectCycle())
            .projectStartedAt(project.getProjectStartedAt())
            .managerName(managerName)
            .managerMemberNo(managerMemberNo)
            .build();
    }

    private ProjectMemberResponse toProjectMemberResponse(ProjectMember pm, LocalDate today) {
        Member m = pm.getMember();
        String todayStatus = resolveTodayAttendanceStatus(m.getMemberNo(), today);
        return ProjectMemberResponse.builder()
            .projectMemberNo(pm.getProjectMemberNo())
            .memberNo(m.getMemberNo())
            .memberName(m.getMemberName())
            .memberEmail(m.getMemberEmail())
            .memberPhone(m.getMemberPhone())
            .projectMemberRole(pm.getProjectMemberRole())
            .memberRole(m.getMemberRole())
            .memberProfileImage(m.getMemberProfileImage())
            .memberStatus(m.getMemberStatus())
            .todayAttendanceStatus(todayStatus)
            .build();
    }

    /** 오늘 ATTENDANCE 마지막 기록 기준: 출근→작업중, 퇴근→작업 종료, 없음→작업 시작 전 */
    private String resolveTodayAttendanceStatus(Long memberNo, LocalDate today) {
        List<com.kh.magamGG.domain.attendance.entity.Attendance> list =
            attendanceRepository.findTodayLastAttendanceByMemberNo(memberNo, today);
        if (list == null || list.isEmpty()) return "작업 시작 전";
        String lastType = list.get(0).getAttendanceType();
        if ("출근".equals(lastType)) return "작업중";
        if ("퇴근".equals(lastType)) return "작업 종료";
        return "작업 시작 전";
    }
}
