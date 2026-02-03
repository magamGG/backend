package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;

import java.util.List;

public interface ProjectService {

    List<ProjectListResponse> getProjectsByMemberNo(Long memberNo);

    /**
     * 에이전시 소속 모든 프로젝트 조회 (에이전시 관리자만 호출 가능)
     */
    List<ProjectListResponse> getProjectsByAgencyNo(Long agencyNo, Long requesterMemberNo);

    ProjectListResponse createProject(ProjectCreateRequest request, Long creatorNo);

    void ensureProjectAccess(Long memberNo, Long projectNo);

    ProjectListResponse getProjectByNo(Long projectNo);

    ProjectListResponse updateProject(Long projectNo, ProjectUpdateRequest request);

    void deleteProject(Long projectNo);

    List<ProjectMemberResponse> getProjectMembers(Long projectNo);

    void addProjectMembers(Long projectNo, List<Long> memberNos);

    void removeProjectMember(Long projectNo, Long projectMemberNo);

    List<ProjectMemberResponse> getAddableMembers(Long projectNo);
}
