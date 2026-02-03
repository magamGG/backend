package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.request.KanbanBoardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.KanbanBoardResponse;
import com.kh.magamGG.domain.project.dto.response.KanbanCardResponse;
import com.kh.magamGG.domain.project.dto.response.TodayTaskResponse;

import java.util.List;

public interface KanbanBoardService {

    /** 아티스트 대시보드 오늘 할 일: 담당자 배정 + 마감일 오늘 + 미완료(N) 카드만 */
    List<TodayTaskResponse> getTodayTasksForMember(Long memberNo);

    List<KanbanBoardResponse> getBoardsByProjectNo(Long projectNo);

    KanbanBoardResponse createBoard(Long projectNo, KanbanBoardCreateRequest request);

    void updateBoardStatus(Long projectNo, Long boardId, KanbanBoardUpdateRequest request);

    KanbanCardResponse createCard(Long projectNo, KanbanCardCreateRequest request);

    KanbanCardResponse updateCard(Long projectNo, Long cardId, KanbanCardUpdateRequest request);
}


