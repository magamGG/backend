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
import com.kh.magamGG.global.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KanbanBoardServiceImpl implements KanbanBoardService {

    private final KanbanBoardRepository kanbanBoardRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<KanbanBoardResponse> getBoardsByProjectNo(Long projectNo) {
        List<KanbanBoard> boards = kanbanBoardRepository.findByProjectNoAndStatusWithCards(projectNo, "Y");
        return boards.stream()
                .map(this::toBoardResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public KanbanBoardResponse createBoard(Long projectNo, KanbanBoardCreateRequest request) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));
        Integer maxOrder = kanbanBoardRepository.findMaxOrderByProjectNo(projectNo);
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;
        KanbanBoard board = KanbanBoard.builder()
                .project(project)
                .kanbanBoardName(request.getTitle() != null ? request.getTitle().trim() : "새 보드")
                .kanbanBoardOrder(nextOrder)
                .kanbanBoardStatus("Y")
                .kanbanCards(Collections.emptyList())
                .build();
        KanbanBoard saved = kanbanBoardRepository.save(board);
        return toBoardResponse(saved);
    }

    @Override
    @Transactional
    public void updateBoardStatus(Long projectNo, Long boardId, KanbanBoardUpdateRequest request) {
        KanbanBoard board = kanbanBoardRepository.findById(boardId)
                .orElseThrow(() -> new ProjectNotFoundException("보드를 찾을 수 없습니다."));
        if (!board.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 보드가 아닙니다.");
        }
        if (request.getStatus() != null && ("Y".equals(request.getStatus()) || "N".equals(request.getStatus()))) {
            board.setKanbanBoardStatus(request.getStatus());
            kanbanBoardRepository.save(board);
        }
    }

    @Override
    @Transactional
    public KanbanCardResponse createCard(Long projectNo, KanbanCardCreateRequest request) {
        if (request.getBoardId() == null) {
            throw new IllegalArgumentException("보드를 선택해주세요.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("카드 제목을 입력해주세요.");
        }
        ProjectMember projectMember;
        if (request.getProjectMemberNo() != null) {
            projectMember = projectMemberRepository.findById(request.getProjectMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));
        } else if (request.getMemberNo() != null) {
            projectMember = projectMemberRepository.findByProject_ProjectNoAndMember_MemberNo(projectNo, request.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("해당 프로젝트의 담당자를 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("담당자를 선택해주세요.");
        }
        if (!projectMember.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("담당자가 이 프로젝트에 속하지 않습니다.");
        }

        KanbanBoard board = kanbanBoardRepository.findById(request.getBoardId())
                .orElseThrow(() -> new ProjectNotFoundException("보드를 찾을 수 없습니다."));
        if (!board.getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("보드가 이 프로젝트에 속하지 않습니다.");
        }

        LocalDate startDate = parseDate(request.getStartDate());
        LocalDate dueDate = parseDate(request.getDueDate());

        LocalDateTime now = LocalDateTime.now();
        KanbanCard card = KanbanCard.builder()
                .kanbanBoard(board)
                .kanbanCardName(request.getTitle().trim())
                .kanbanCardDescription(request.getDescription() != null ? request.getDescription().trim() : null)
                .kanbanCardStatus("N")
                .kanbanCardCreatedAt(now)
                .kanbanCardUpdatedAt(now)
                .projectMember(projectMember)
                .kanbanCardStartedAt(startDate)
                .kanbanCardEndedAt(dueDate)
                .build();

        KanbanCard saved = kanbanCardRepository.save(card);
        return toCardResponse(saved);
    }

    @Override
    @Transactional
    public KanbanCardResponse updateCard(Long projectNo, Long cardId, KanbanCardUpdateRequest request) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
                .orElseThrow(() -> new ProjectNotFoundException("카드를 찾을 수 없습니다."));
        if (!card.getKanbanBoard().getProject().getProjectNo().equals(projectNo)) {
            throw new IllegalArgumentException("해당 프로젝트의 카드가 아닙니다.");
        }
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            card.setKanbanCardName(request.getTitle().trim());
        }
        // description: null이면 미갱신, "" 또는 값이면 저장 (빈 문자열은 null로)
        if (request.getDescription() != null) {
            String desc = request.getDescription().trim();
            card.setKanbanCardDescription(desc.isEmpty() ? null : desc);
        }
        if (request.getStartDate() != null) {
            card.setKanbanCardStartedAt(parseDate(request.getStartDate()));
        }
        if (request.getDueDate() != null) {
            card.setKanbanCardEndedAt(parseDate(request.getDueDate()));
        }
        // status "D" = 삭제(숨김), completed = Y(체크)/N(언체크)
        if (request.getStatus() != null && "D".equalsIgnoreCase(request.getStatus().trim())) {
            card.setKanbanCardStatus("D");
        } else if (request.getCompleted() != null) {
            card.setKanbanCardStatus(Boolean.TRUE.equals(request.getCompleted()) ? "Y" : "N");
        }
        if (request.getProjectMemberNo() != null || request.getMemberNo() != null) {
            ProjectMember projectMember;
            if (request.getProjectMemberNo() != null) {
                projectMember = projectMemberRepository.findById(request.getProjectMemberNo())
                        .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));
            } else {
                projectMember = projectMemberRepository.findByProject_ProjectNoAndMember_MemberNo(projectNo, request.getMemberNo())
                        .orElseThrow(() -> new IllegalArgumentException("해당 프로젝트의 담당자를 찾을 수 없습니다."));
            }
            if (projectMember.getProject().getProjectNo().equals(projectNo)) {
                card.setProjectMember(projectMember);
            }
        }
        if (request.getBoardId() != null && !request.getBoardId().equals(card.getKanbanBoard().getKanbanBoardNo())) {
            KanbanBoard newBoard = kanbanBoardRepository.findById(request.getBoardId())
                    .orElseThrow(() -> new ProjectNotFoundException("보드를 찾을 수 없습니다."));
            if (newBoard.getProject().getProjectNo().equals(projectNo)) {
                card.setKanbanBoard(newBoard);
            }
        }
        card.setKanbanCardUpdatedAt(LocalDateTime.now());
        KanbanCard saved = kanbanCardRepository.save(card);
        return toCardResponse(saved);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private KanbanBoardResponse toBoardResponse(KanbanBoard board) {
        List<KanbanCardResponse> cards = board.getKanbanCards().stream()
                .filter(c -> "Y".equals(c.getKanbanCardStatus()) || "N".equals(c.getKanbanCardStatus()))
                .map(this::toCardResponse)
                .collect(Collectors.toList());
        return KanbanBoardResponse.builder()
                .id(board.getKanbanBoardNo())
                .title(board.getKanbanBoardName() != null ? board.getKanbanBoardName() : "")
                .cards(cards)
                .build();
    }

    private KanbanCardResponse toCardResponse(KanbanCard card) {
        KanbanCardResponse.AssigneeInfo assignee = null;
        if (card.getProjectMember() != null && card.getProjectMember().getMember() != null) {
            var m = card.getProjectMember().getMember();
            assignee = KanbanCardResponse.AssigneeInfo.builder()
                    .id(m.getMemberNo())
                    .name(m.getMemberName())
                    .role(card.getProjectMember().getProjectMemberRole())
                    .email(m.getMemberEmail())
                    .avatar(m.getMemberProfileImage())
                    .build();
        }
        return KanbanCardResponse.builder()
                .id(card.getKanbanCardNo())
                .title(card.getKanbanCardName())
                .description(card.getKanbanCardDescription() != null ? card.getKanbanCardDescription() : "")
                .startDate(card.getKanbanCardStartedAt() != null ? card.getKanbanCardStartedAt().toString() : null)
                .dueDate(card.getKanbanCardEndedAt() != null ? card.getKanbanCardEndedAt().toString() : null)
                .boardId(card.getKanbanBoard().getKanbanBoardNo())
                .completed("Y".equals(card.getKanbanCardStatus()))
                .assignedTo(assignee)
                .createdAt(card.getKanbanCardCreatedAt() != null ? card.getKanbanCardCreatedAt().toString() : null)
                .build();
    }
}


