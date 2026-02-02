package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;

import java.util.List;

public interface ProjectService {

    /**
     * 로그인 회원 기준 프로젝트 목록 조회 (PROJECT_MEMBER 소속 프로젝트)
     */
    List<ProjectListResponse> getProjectsByMemberNo(Long memberNo);

    /**
     * 프로젝트 생성 (PROJECT + PROJECT_MEMBER 동시 생성)
     *
     * @param request           프로젝트 생성 요청
     * @param creatorMemberNo   생성자 회원 번호 (X-Member-No)
     * @return 생성된 프로젝트 목록 응답
     */
    ProjectListResponse createProject(ProjectCreateRequest request, Long creatorMemberNo);

    /**
     * 프로젝트 단건 조회
     */
    ProjectListResponse getProjectByNo(Long projectNo);

    /**
     * 프로젝트 수정
     */
    ProjectListResponse updateProject(Long projectNo, ProjectUpdateRequest request);

    /**
     * 프로젝트 삭제
     */
    void deleteProject(Long projectNo);

    /**
     * 프로젝트 멤버 목록 조회
     */
    List<ProjectMemberResponse> getProjectMembers(Long projectNo);

    /**
     * 프로젝트에 추가 가능한 회원 목록 (담당자/작가 제외, 프로젝트 미소속)
     */
    List<ProjectMemberResponse> getAddableMembers(Long projectNo);

    /**
     * 프로젝트에 팀원 추가 (PROJECT_MEMBER에 어시스트 역할로 등록)
     */
    void addProjectMembers(Long projectNo, List<Long> memberNos);

    /**
     * 프로젝트 접근 권한 검증 (PROJECT_MEMBER 소속 또는 에이전시 관리자만 접근 가능)
     * 권한 없으면 ProjectAccessDeniedException 발생
     */
    void ensureProjectAccess(Long memberNo, Long projectNo);
}


