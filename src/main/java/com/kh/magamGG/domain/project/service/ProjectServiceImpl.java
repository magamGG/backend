package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;

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
    @Transactional
    public ProjectListResponse createProject(ProjectCreateRequest request, Long creatorNo) {
        Project project = new Project();
        project.setProjectName(request.getProjectName() != null ? request.getProjectName() : "제목 없음");
        project.setProjectStatus(request.getProjectStatus() != null ? request.getProjectStatus() : "연재");
        project.setProjectColor(request.getProjectColor() != null ? request.getProjectColor() : "기본색");
        project.setProjectCycle(request.getProjectCycle());
        project.setPlatform(request.getPlatform());
        project.setThumbnailFile(request.getThumbnailFile());
        project.setProjectStartedAt(request.getProjectStartedAt());
        Project saved = projectRepository.save(project);

        Member artist = memberRepository.findById(request.getArtistMemberNo())
            .orElseThrow(() -> new IllegalArgumentException("작가 회원을 찾을 수 없습니다: " + request.getArtistMemberNo()));
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
        boolean exists = projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo);
        if (!exists) {
            throw new IllegalArgumentException("해당 프로젝트에 대한 접근 권한이 없습니다.");
        }
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
        for (Long memberNo : memberNos) {
            if (projectMemberRepository.existsByProject_ProjectNoAndMember_MemberNo(projectNo, memberNo)) continue;
            Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberNo));
            ProjectMember pm = new ProjectMember();
            pm.setMember(member);
            pm.setProject(project);
            pm.setProjectMemberRole("어시스트");
            projectMemberRepository.save(pm);
        }
    }

    @Override
    public List<ProjectMemberResponse> getAddableMembers(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        List<ProjectMember> existing = projectMemberRepository.findByProject_ProjectNo(projectNo);
        if (existing.isEmpty()) return List.of();
        Long agencyNo = existing.get(0).getMember().getAgency().getAgencyNo();
        Set<Long> existingMemberNos = existing.stream().map(pm -> pm.getMember().getMemberNo()).collect(Collectors.toSet());
        List<String> excludeRoles = List.of("담당자", "웹툰 작가", "웹소설 작가");
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
        for (ProjectMember pm : members) {
            if ("작가".equals(pm.getProjectMemberRole())) {
                artistName = pm.getMember().getMemberName();
                artistMemberNo = pm.getMember().getMemberNo();
                break;
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
