package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.KanbanBoardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.KanbanBoardResponse;
import com.kh.magamGG.domain.project.dto.response.KanbanCardResponse;

import java.util.List;

public interface KanbanBoardService {

    KanbanBoardResponse createBoard(Long projectNo, KanbanBoardCreateRequest request);

    void updateBoardStatus(Long projectNo, Long boardId, KanbanBoardUpdateRequest request);

    KanbanCardResponse createCard(Long projectNo, KanbanCardCreateRequest request);

    KanbanCardResponse updateCard(Long projectNo, Long cardId, KanbanCardUpdateRequest request);

    List<KanbanBoardResponse> getBoardsByProjectNo(Long projectNo);
}
