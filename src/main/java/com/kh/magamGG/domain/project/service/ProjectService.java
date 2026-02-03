package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;

import java.util.List;

public interface ProjectService {

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
