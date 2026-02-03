package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;

import java.util.List;

public interface ProjectService {

    /**
     * 담당자의 담당 작가들의 프로젝트 목록 (마감 기한 대비 진행률 → 정상/주의)
     * @param memberNo 로그인한 담당자 회원 번호
     */
    List<ManagedProjectResponse> getManagedProjectsByManager(Long memberNo);

    List<ProjectListResponse> getProjectsByMemberNo(Long memberNo);

    ProjectListResponse createProject(ProjectCreateRequest request, Long creatorNo);

    void ensureProjectAccess(Long memberNo, Long projectNo);

    ProjectListResponse getProjectByNo(Long projectNo);

    ProjectListResponse updateProject(Long projectNo, ProjectUpdateRequest request);

    void deleteProject(Long projectNo);

    List<ProjectMemberResponse> getProjectMembers(Long projectNo);

    void addProjectMembers(Long projectNo, List<Long> memberNos);

    List<ProjectMemberResponse> getAddableMembers(Long projectNo);
}
