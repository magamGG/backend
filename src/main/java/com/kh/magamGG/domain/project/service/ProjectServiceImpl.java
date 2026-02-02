package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import com.kh.magamGG.global.exception.MemberNotFoundException;
import com.kh.magamGG.global.exception.ProjectAccessDeniedException;
import com.kh.magamGG.global.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Set.of;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    /** 프로젝트 내 역할: 담당자 / 작가 / 어시스트 (이 역할만 프로젝트 조회 가능) */
    private static final String ROLE_ARTIST = "작가";
    private static final String ROLE_MANAGER = "담당자";
    private static final String ROLE_ASSISTANT = "어시스트";
    private static final Set<String> VIEWER_ROLES = of("작가", "담당자", "어시스트");
    private static final String ROLE_AGENCY_ADMIN = "에이전시 관리자";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<ProjectListResponse> getProjectsByMemberNo(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        // 에이전시 관리자는 소속 에이전시의 모든 프로젝트 조회 가능
        if (ROLE_AGENCY_ADMIN.equals(member.getMemberRole()) && member.getAgency() != null) {
            return projectRepository.findAllByAgencyNo(member.getAgency().getAgencyNo()).stream()
                    .map(this::toProjectListResponse)
                    .collect(Collectors.toList());
        }

        // 그 외: PROJECT_MEMBER에서 담당자/작가/어시스트로 포함된 프로젝트만 조회
        Set<Long> seenProjectNos = new HashSet<>();
        return projectMemberRepository.findByMember_MemberNo(memberNo).stream()
                .filter(pm -> VIEWER_ROLES.contains(pm.getProjectMemberRole()))
                .map(ProjectMember::getProject)
                .filter(p -> seenProjectNos.add(p.getProjectNo()))
                .map(this::toProjectListResponse)
                .collect(Collectors.toList());
    }

    private ProjectListResponse toProjectListResponse(Project project) {
        String artistName = project.getProjectMembers().stream()
                .filter(pm -> ROLE_ARTIST.equals(pm.getProjectMemberRole()))
                .map(pm -> pm.getMember().getMemberName())
                .findFirst()
                .orElseGet(() -> project.getProjectMembers().isEmpty()
                        ? null
                        : project.getProjectMembers().get(0).getMember().getMemberName());

        Long artistMemberNo = project.getProjectMembers().stream()
                .filter(pm -> ROLE_ARTIST.equals(pm.getProjectMemberRole()))
                .map(pm -> pm.getMember().getMemberNo())
                .findFirst()
                .orElseGet(() -> project.getProjectMembers().isEmpty()
                        ? null
                        : project.getProjectMembers().get(0).getMember().getMemberNo());

        return ProjectListResponse.builder()
                .projectNo(project.getProjectNo())
                .projectName(project.getProjectName())
                .projectStatus(project.getProjectStatus())
                .projectColor(project.getProjectColor())
                .thumbnailFile(project.getThumbnailFile())
                .artistName(artistName)
                .artistMemberNo(artistMemberNo)
                .projectGenre(project.getProjectGenre())
                .platform(project.getPlatform())
                .projectCycle(project.getProjectCycle())
                .projectStartedAt(project.getProjectStartedAt())
                .build();
    }

    @Override
    public void ensureProjectAccess(Long memberNo, Long projectNo) {
        if (memberNo == null) {
            throw new ProjectAccessDeniedException("접근 권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));
        if (ROLE_AGENCY_ADMIN.equals(member.getMemberRole()) && member.getAgency() != null) {
            boolean hasAgencyProject = projectRepository.findAllByAgencyNo(member.getAgency().getAgencyNo()).stream()
                    .anyMatch(p -> p.getProjectNo().equals(projectNo));
            if (hasAgencyProject) return;
        }
        if (!projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo)) {
            throw new ProjectAccessDeniedException("이 프로젝트에 대한 접근 권한이 없습니다.");
        }
    }

    @Override
    public ProjectListResponse getProjectByNo(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));
        return toProjectListResponse(project);
    }

    @Override
    @Transactional
    public ProjectListResponse updateProject(Long projectNo, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));

        if (request.getProjectName() != null) {
            project.setProjectName(request.getProjectName());
        }
        if (request.getProjectStatus() != null) {
            project.setProjectStatus(request.getProjectStatus());
        }
        if (request.getProjectColor() != null) {
            project.setProjectColor(request.getProjectColor());
        }
        if (request.getProjectCycle() != null) {
            project.setProjectCycle(request.getProjectCycle());
        }
        if (request.getThumbnailFile() != null) {
            project.setThumbnailFile(request.getThumbnailFile());
        }
        if (request.getProjectStartedAt() != null) {
            project.setProjectStartedAt(request.getProjectStartedAt());
        }
        if (request.getProjectGenre() != null) {
            project.setProjectGenre(request.getProjectGenre());
        }
        if (request.getPlatform() != null) {
            project.setPlatform(request.getPlatform());
        }

        project = projectRepository.save(project);
        return toProjectListResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectMemberResponse> getProjectMembers(Long projectNo) {
        projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));

        return projectMemberRepository.findByProject_ProjectNo(projectNo).stream()
                .map(pm -> ProjectMemberResponse.builder()
                        .projectMemberNo(pm.getProjectMemberNo())
                        .memberNo(pm.getMember().getMemberNo())
                        .memberName(pm.getMember().getMemberName())
                        .memberEmail(pm.getMember().getMemberEmail())
                        .memberPhone(pm.getMember().getMemberPhone())
                        .projectMemberRole(pm.getProjectMemberRole())
                        .memberRole(pm.getMember().getMemberRole())
                        .memberProfileImage(pm.getMember().getMemberProfileImage())
                        .memberStatus(pm.getMember().getMemberStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectMemberResponse> getAddableMembers(Long projectNo) {
        if (!projectRepository.existsById(projectNo)) {
            throw new ProjectNotFoundException("프로젝트를 찾을 수 없습니다.");
        }

        return memberRepository.findAddableMembersByProject(projectNo).stream()
                .map(m -> ProjectMemberResponse.builder()
                        .memberNo(m.getMemberNo())
                        .memberName(m.getMemberName())
                        .memberEmail(m.getMemberEmail())
                        .memberPhone(m.getMemberPhone())
                        .projectMemberRole(m.getMemberRole())
                        .memberRole(m.getMemberRole())
                        .memberProfileImage(m.getMemberProfileImage())
                        .memberStatus(m.getMemberStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectListResponse createProject(ProjectCreateRequest request, Long creatorMemberNo) {
        // 작가 존재 여부 검증
        Member artist = memberRepository.findById(request.getArtistMemberNo())
                .orElseThrow(() -> new MemberNotFoundException("선택한 작가를 찾을 수 없습니다."));

        // 생성자 존재 여부 검증
        Member creator = memberRepository.findById(creatorMemberNo)
                .orElseThrow(() -> new MemberNotFoundException("생성자 정보를 찾을 수 없습니다."));

        // 1. PROJECT 생성 및 저장
        Project project = Project.builder()
                .projectName(request.getProjectName())
                .projectStatus(request.getProjectStatus() != null ? request.getProjectStatus() : "연재")
                .projectColor(request.getProjectColor() != null ? request.getProjectColor() : "기본색")
                .projectCycle(request.getProjectCycle())
                .thumbnailFile(request.getThumbnailFile())
                .projectStartedAt(request.getProjectStartedAt())
                .projectGenre(request.getProjectGenre())
                .platform(request.getPlatform())
                .build();

        project = projectRepository.save(project);

        // 2. PROJECT_MEMBER: 작가 등록 (역할: 작가)
        ProjectMember artistMember = ProjectMember.builder()
                .member(artist)
                .project(project)
                .projectMemberRole(ROLE_ARTIST)
                .build();
        projectMemberRepository.save(artistMember);

        // 3. 생성자 != 작가인 경우, 생성자를 담당자로 추가
        if (!creatorMemberNo.equals(request.getArtistMemberNo())) {
            ProjectMember creatorMember = ProjectMember.builder()
                    .member(creator)
                    .project(project)
                    .projectMemberRole(ROLE_MANAGER)
                    .build();
            projectMemberRepository.save(creatorMember);
        }

        return ProjectListResponse.builder()
                .projectNo(project.getProjectNo())
                .projectName(project.getProjectName())
                .projectStatus(project.getProjectStatus())
                .projectColor(project.getProjectColor())
                .thumbnailFile(project.getThumbnailFile())
                .artistName(artist.getMemberName())
                .artistMemberNo(artist.getMemberNo())
                .projectGenre(project.getProjectGenre())
                .platform(project.getPlatform())
                .projectCycle(project.getProjectCycle())
                .projectStartedAt(project.getProjectStartedAt())
                .build();
    }

    @Override
    @Transactional
    public void addProjectMembers(Long projectNo, List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            return;
        }
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));
        for (Long memberNo : memberNos) {
            if (projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo)) {
                continue;
            }
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다. memberNo=" + memberNo));
            ProjectMember pm = ProjectMember.builder()
                    .project(project)
                    .member(member)
                    .projectMemberRole(ROLE_ASSISTANT)
                    .build();
            projectMemberRepository.save(pm);
        }
    }
}
