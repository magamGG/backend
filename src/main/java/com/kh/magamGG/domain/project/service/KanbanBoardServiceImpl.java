package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.KanbanBoardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.KanbanBoardResponse;
import com.kh.magamGG.domain.project.dto.response.KanbanCardResponse;
import com.kh.magamGG.domain.project.entity.KanbanBoard;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.KanbanBoardRepository;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanBoardServiceImpl implements KanbanBoardService {

    private final ProjectRepository projectRepository;
    private final KanbanBoardRepository kanbanBoardRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final ProjectMemberRepository projectMemberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    @Transactional
    public KanbanBoardResponse createBoard(Long projectNo, KanbanBoardCreateRequest request) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        List<KanbanBoard> boards = kanbanBoardRepository.findByProject_ProjectNoOrderByKanbanBoardOrderAsc(projectNo);
        int nextOrder = boards.size();
        KanbanBoard board = new KanbanBoard();
        board.setProject(project);
        board.setKanbanBoardName(request.getTitle() != null ? request.getTitle() : "보드");
        board.setKanbanBoardOrder(nextOrder);
        board.setKanbanBoardStatus("Y");
        KanbanBoard saved = kanbanBoardRepository.save(board);
        return toBoardResponse(saved);
    }

    @Override
    @Transactional
    public void updateBoardStatus(Long projectNo, Long boardId, KanbanBoardUpdateRequest request) {
        KanbanBoard board = kanbanBoardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다: " + boardId));
        if (!board.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 보드가 아닙니다.");
        }
        if (request.getStatus() != null) {
            board.setKanbanBoardStatus("N".equalsIgnoreCase(request.getStatus()) ? "N" : "Y");
        }
        kanbanBoardRepository.save(board);
    }

    @Override
    @Transactional
    public KanbanCardResponse createCard(Long projectNo, KanbanCardCreateRequest request) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        Long boardId = request.getBoardId() != null ? request.getBoardId() : getFirstBoardId(projectNo);
        KanbanBoard board = kanbanBoardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다: " + boardId));
        if (!board.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 보드가 아닙니다.");
        }
        ProjectMember assignee = resolveProjectMember(projectNo, request.getProjectMemberNo(), request.getMemberNo());
        KanbanCard card = new KanbanCard();
        card.setKanbanBoard(board);
        card.setKanbanCardName(request.getTitle() != null ? request.getTitle() : "카드");
        card.setKanbanCardStatus("N");
        card.setProjectMember(assignee);
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            card.setKanbanCardStartedAt(LocalDate.parse(request.getStartDate(), DATE_FMT));
        }
        if (request.getDueDate() != null && !request.getDueDate().isEmpty()) {
            card.setKanbanCardEndedAt(LocalDate.parse(request.getDueDate(), DATE_FMT));
        }
        KanbanCard saved = kanbanCardRepository.save(card);
        return toCardResponse(saved);
    }

    @Override
    @Transactional
    public KanbanCardResponse updateCard(Long projectNo, Long cardId, KanbanCardUpdateRequest request) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다: " + cardId));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        if (request.getStatus() != null && "D".equalsIgnoreCase(request.getStatus())) {
            card.setKanbanCardStatus("N");
            kanbanCardRepository.save(card);
            return toCardResponse(card);
        }
        if (request.getTitle() != null) card.setKanbanCardName(request.getTitle());
        if (request.getCompleted() != null) card.setKanbanCardStatus(request.getCompleted() ? "Y" : "N");
        if (request.getBoardId() != null) {
            KanbanBoard board = kanbanBoardRepository.findById(request.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("보드를 찾을 수 없습니다: " + request.getBoardId()));
            if (board.getProject().getProjectNo().equals(projectNo)) card.setKanbanBoard(board);
        }
        if (request.getProjectMemberNo() != null || request.getMemberNo() != null) {
            ProjectMember assignee = resolveProjectMember(projectNo, request.getProjectMemberNo(), request.getMemberNo());
            if (assignee != null) card.setProjectMember(assignee);
        }
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            card.setKanbanCardStartedAt(LocalDate.parse(request.getStartDate(), DATE_FMT));
        }
        if (request.getDueDate() != null && !request.getDueDate().isEmpty()) {
            card.setKanbanCardEndedAt(LocalDate.parse(request.getDueDate(), DATE_FMT));
        }
        KanbanCard saved = kanbanCardRepository.save(card);
        return toCardResponse(saved);
    }

    @Override
    public List<KanbanBoardResponse> getBoardsByProjectNo(Long projectNo) {
        List<KanbanBoard> boards = kanbanBoardRepository.findByProject_ProjectNoAndKanbanBoardStatusOrderByKanbanBoardOrderAsc(projectNo, "Y");
        return boards.stream().map(this::toBoardResponse).collect(Collectors.toList());
    }

    private Long getFirstBoardId(Long projectNo) {
        List<KanbanBoard> boards = kanbanBoardRepository.findByProject_ProjectNoOrderByKanbanBoardOrderAsc(projectNo);
        if (boards.isEmpty()) throw new IllegalArgumentException("프로젝트에 보드가 없습니다. 먼저 보드를 추가하세요.");
        return boards.get(0).getKanbanBoardNo();
    }

    private ProjectMember resolveProjectMember(Long projectNo, Long projectMemberNo, Long memberNo) {
        if (projectMemberNo != null) {
            return projectMemberRepository.findById(projectMemberNo)
                .filter(pm -> pm.getProject().getProjectNo().equals(projectNo))
                .orElseThrow(() -> new IllegalArgumentException("프로젝트 멤버를 찾을 수 없습니다: " + projectMemberNo));
        }
        if (memberNo != null) {
            return projectMemberRepository.findByProject_ProjectNo(projectNo).stream()
                .filter(pm -> pm.getMember().getMemberNo().equals(memberNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 프로젝트에 소속되어 있지 않습니다: " + memberNo));
        }
        return projectMemberRepository.findByProject_ProjectNo(projectNo).stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("프로젝트에 멤버가 없습니다."));
    }

    private KanbanBoardResponse toBoardResponse(KanbanBoard board) {
        List<KanbanCardResponse> cards = board.getKanbanCards().stream()
            .map(this::toCardResponse)
            .collect(Collectors.toList());
        return KanbanBoardResponse.builder()
            .id(board.getKanbanBoardNo())
            .title(board.getKanbanBoardName())
            .cards(cards)
            .build();
    }

    private KanbanCardResponse toCardResponse(KanbanCard card) {
        ProjectMember pm = card.getProjectMember();
        KanbanCardResponse.AssigneeInfo assignee = null;
        if (pm != null) {
            assignee = KanbanCardResponse.AssigneeInfo.builder()
                .id(pm.getProjectMemberNo())
                .name(pm.getMember().getMemberName())
                .role(pm.getProjectMemberRole())
                .email(pm.getMember().getMemberEmail())
                .avatar(pm.getMember().getMemberProfileImage())
                .build();
        }
        return KanbanCardResponse.builder()
            .id(card.getKanbanCardNo())
            .title(card.getKanbanCardName())
            .description(null)
            .startDate(card.getKanbanCardStartedAt() != null ? card.getKanbanCardStartedAt().format(DATE_FMT) : null)
            .dueDate(card.getKanbanCardEndedAt() != null ? card.getKanbanCardEndedAt().format(DATE_FMT) : null)
            .boardId(card.getKanbanBoard().getKanbanBoardNo())
            .completed("Y".equals(card.getKanbanCardStatus()))
            .assignedTo(assignee)
            .createdAt(card.getKanbanCardCreatedAt() != null ? card.getKanbanCardCreatedAt().toString() : null)
            .build();
    }
}
