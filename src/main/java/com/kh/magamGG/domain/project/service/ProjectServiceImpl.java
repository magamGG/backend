package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
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

            result.add(ManagedProjectResponse.builder()
                    .projectNo(project.getProjectNo())
                    .projectName(project.getProjectName())
                    .artist(artistName)
                    .status(status)
                    .progress(progress)
                    .deadline(deadlineStr)
                    .build());
        }

        result.sort(Comparator.comparing(ManagedProjectResponse::getProjectNo));
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
            LocalDate startDate = startedAt.toLocalDate();
            int cycleDays = (p.getProjectCycle() != null && p.getProjectCycle() > 0) ? p.getProjectCycle() : 7;

            long daysBetween = ChronoUnit.DAYS.between(startDate, today);
            int n = daysBetween <= 0 ? 0 : (int) Math.ceil((double) daysBetween / cycleDays);
            LocalDate nextDate = startDate.plusDays((long) n * cycleDays);

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
        return list.stream().map(this::toProjectMemberResponse).collect(Collectors.toList());
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

    private ProjectMemberResponse toProjectMemberResponse(ProjectMember pm) {
        Member m = pm.getMember();
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
            .build();
    }
}
