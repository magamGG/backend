package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.dto.response.NextSerialProjectItemResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;

import java.util.List;

public interface ProjectService {

    /**
     * 담당자의 담당 작가들의 프로젝트 목록 (마감 기한 대비 진행률 → 정상/주의)
     * @param memberNo 로그인한 담당자 회원 번호
     */
    List<ManagedProjectResponse> getManagedProjectsByManager(Long memberNo);

    /**
     * 아티스트 대시보드 "다음 연재 프로젝트": PROJECT_MEMBER 소속 + PROJECT_STARTED_AT, PROJECT_CYCLE로 계산한 다음 연재일 목록
     */
    List<NextSerialProjectItemResponse> getNextSerialProjectsForMember(Long memberNo, int limit);

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
