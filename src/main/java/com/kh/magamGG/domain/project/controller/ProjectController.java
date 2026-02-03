package com.kh.magamGG.domain.project.controller;

import com.kh.magamGG.domain.project.dto.request.KanbanBoardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentCreateRequest;
import com.kh.magamGG.domain.project.dto.request.CommentUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectCreateRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectMemberAddRequest;
import com.kh.magamGG.domain.project.dto.request.ProjectUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.KanbanBoardResponse;
import com.kh.magamGG.domain.project.dto.response.CommentResponse;
import com.kh.magamGG.domain.project.dto.response.KanbanCardResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectListResponse;
import com.kh.magamGG.domain.project.dto.response.ProjectMemberResponse;
import com.kh.magamGG.domain.project.service.CommentService;
import com.kh.magamGG.domain.project.service.KanbanBoardService;
import com.kh.magamGG.domain.project.service.ProjectService;
import com.kh.magamGG.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 프로젝트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final KanbanBoardService kanbanBoardService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;

    /**
     * 프로젝트 목록 조회 (로그인 회원 기준, PROJECT_MEMBER 소속 프로젝트)
     * GET /api/projects
     *
     * @param memberNo 회원 번호 (X-Member-No 헤더)
     */
    @GetMapping
    public ResponseEntity<List<ProjectListResponse>> getProjects(
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        List<ProjectListResponse> list = projectService.getProjectsByMemberNo(memberNo);
        return ResponseEntity.ok(list);
    }

    /**
     * 에이전시 소속 전체 프로젝트 조회 (에이전시 관리자만 가능)
     * GET /api/projects/agency/{agencyNo}
     */
    @GetMapping("/agency/{agencyNo}")
    public ResponseEntity<List<ProjectListResponse>> getProjectsByAgency(
            @PathVariable Long agencyNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        List<ProjectListResponse> list = projectService.getProjectsByAgencyNo(agencyNo, memberNo);
        return ResponseEntity.ok(list);
    }

    /**
     * 프로젝트 썸네일 업로드 (프로젝트 생성 전 호출)
     * POST /api/projects/upload-thumbnail
     *
     * @return 저장된 파일명 (예: uuid.jpg) - createProject 요청 시 thumbnailFile에 전달
     */
    @PostMapping("/upload-thumbnail")
    public ResponseEntity<String> uploadThumbnail(
            @RequestParam("file") MultipartFile file
    ) {
        String fileName = fileStorageService.saveFile(file);
        return ResponseEntity.ok(fileName);
    }

    /**
     * 프로젝트 생성
     * POST /api/projects
     *
     * @param request     프로젝트 생성 요청 (projectName, artistMemberNo 필수)
     * @param creatorNo   생성자 회원 번호 (X-Member-No 헤더)
     */
    @PostMapping
    public ResponseEntity<ProjectListResponse> createProject(
            @RequestBody ProjectCreateRequest request,
            @RequestHeader("X-Member-No") Long creatorNo
    ) {
        ProjectListResponse response = projectService.createProject(request, creatorNo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 프로젝트 단건 조회 (PROJECT_MEMBER 소속만 접근 가능)
     * GET /api/projects/{projectNo}
     */
    @GetMapping("/{projectNo}")
    public ResponseEntity<ProjectListResponse> getProject(
            @PathVariable Long projectNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        ProjectListResponse response = projectService.getProjectByNo(projectNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로젝트 수정
     * PUT /api/projects/{projectNo}
     */
    @PutMapping("/{projectNo}")
    public ResponseEntity<ProjectListResponse> updateProject(
            @PathVariable Long projectNo,
            @RequestBody ProjectUpdateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        ProjectListResponse response = projectService.updateProject(projectNo, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로젝트 삭제
     * DELETE /api/projects/{projectNo}
     */
    @DeleteMapping("/{projectNo}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        projectService.deleteProject(projectNo);
        return ResponseEntity.noContent().build();
    }

    /**
     * 프로젝트 멤버 목록 조회
     * GET /api/projects/{projectNo}/members
     */
    @GetMapping("/{projectNo}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(
            @PathVariable Long projectNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        List<ProjectMemberResponse> members = projectService.getProjectMembers(projectNo);
        return ResponseEntity.ok(members);
    }

    /**
     * 프로젝트에 팀원 추가 (PROJECT_MEMBER 테이블에 어시스트 역할로 등록)
     * POST /api/projects/{projectNo}/members
     */
    @PostMapping("/{projectNo}/members")
    public ResponseEntity<Void> addProjectMembers(
            @PathVariable Long projectNo,
            @RequestBody ProjectMemberAddRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        List<Long> memberNos = request.getMemberNos();
        if (memberNos != null && !memberNos.isEmpty()) {
            projectService.addProjectMembers(projectNo, memberNos);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 프로젝트에서 팀원 삭제 (PROJECT_MEMBER 테이블에서 삭제)
     * DELETE /api/projects/{projectNo}/members/remove/{projectMemberNo}
     */
    @DeleteMapping("/{projectNo}/members/remove/{projectMemberNo}")
    public ResponseEntity<Void> removeProjectMember(
            @PathVariable Long projectNo,
            @PathVariable Long projectMemberNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        projectService.removeProjectMember(projectNo, projectMemberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 프로젝트에 추가 가능한 회원 목록 (MEMBER_ROLE != 담당자/작가, 프로젝트 미소속)
     * GET /api/projects/{projectNo}/addable-members
     */
    @GetMapping("/{projectNo}/addable-members")
    public ResponseEntity<List<ProjectMemberResponse>> getAddableMembers(
            @PathVariable Long projectNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        List<ProjectMemberResponse> members = projectService.getAddableMembers(projectNo);
        return ResponseEntity.ok(members);
    }

    /**
     * 칸반 보드 추가 (KANBAN_BOARD 테이블에 INSERT)
     * POST /api/projects/{projectNo}/kanban-board
     */
    @PostMapping("/{projectNo}/kanban-board")
    public ResponseEntity<KanbanBoardResponse> createKanbanBoard(
            @PathVariable Long projectNo,
            @RequestBody KanbanBoardCreateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        KanbanBoardResponse created = kanbanBoardService.createBoard(projectNo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 칸반 보드 상태 수정 (KANBAN_BOARD_STATUS N으로 설정 시 숨김)
     * PUT /api/projects/{projectNo}/kanban-board/{boardId}
     */
    @PutMapping("/{projectNo}/kanban-board/{boardId}")
    public ResponseEntity<Void> updateKanbanBoardStatus(
            @PathVariable Long projectNo,
            @PathVariable Long boardId,
            @RequestBody KanbanBoardUpdateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        kanbanBoardService.updateBoardStatus(projectNo, boardId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 칸반 카드 추가 (KANBAN_CARD INSERT)
     * POST /api/projects/{projectNo}/kanban-card
     */
    @PostMapping("/{projectNo}/kanban-card")
    public ResponseEntity<KanbanCardResponse> createKanbanCard(
            @PathVariable Long projectNo,
            @RequestBody KanbanCardCreateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        KanbanCardResponse created = kanbanBoardService.createCard(projectNo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 칸반 카드 수정 (KANBAN_CARD UPDATE)
     * PUT /api/projects/{projectNo}/kanban-card/{cardId}
     */
    /**
     * 칸반 카드 코멘트 목록 조회 (COMMENT - KANBAN_CARD_NO 기준)
     * GET /api/projects/{projectNo}/kanban-card/{cardId}/comments
     */
    @GetMapping("/{projectNo}/kanban-card/{cardId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long projectNo,
            @PathVariable Long cardId,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        List<CommentResponse> comments = commentService.getCommentsByCardId(projectNo, cardId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 칸반 카드 코멘트 추가 (COMMENT INSERT)
     * POST /api/projects/{projectNo}/kanban-card/{cardId}/comments
     */
    @PostMapping("/{projectNo}/kanban-card/{cardId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long projectNo,
            @PathVariable Long cardId,
            @RequestBody CommentCreateRequest request,
            @RequestHeader(value = "X-Member-No", required = true) Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        CommentResponse created = commentService.createComment(projectNo, cardId, memberNo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 칸반 카드 코멘트 수정 (COMMENT UPDATE)
     * PUT /api/projects/{projectNo}/kanban-card/{cardId}/comments/{commentId}
     */
    @PutMapping("/{projectNo}/kanban-card/{cardId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long projectNo,
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        CommentResponse updated = commentService.updateComment(projectNo, cardId, commentId, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{projectNo}/kanban-card/{cardId}")
    public ResponseEntity<KanbanCardResponse> updateKanbanCard(
            @PathVariable Long projectNo,
            @PathVariable Long cardId,
            @RequestBody KanbanCardUpdateRequest request,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        KanbanCardResponse updated = kanbanBoardService.updateCard(projectNo, cardId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 칸반 보드 목록 조회 (KANBAN_BOARD, KANBAN_CARD 기준)
     * GET /api/projects/{projectNo}/kanban
     */
    @GetMapping("/{projectNo}/kanban")
    public ResponseEntity<List<KanbanBoardResponse>> getKanbanBoards(
            @PathVariable Long projectNo,
            @RequestHeader("X-Member-No") Long memberNo
    ) {
        projectService.ensureProjectAccess(memberNo, projectNo);
        List<KanbanBoardResponse> boards = kanbanBoardService.getBoardsByProjectNo(projectNo);
        return ResponseEntity.ok(boards);
    }
}
