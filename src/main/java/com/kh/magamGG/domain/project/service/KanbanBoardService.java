package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.member.dto.MemberKanbanStatsResponseDto;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanBoardUpdateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardCreateRequest;
import com.kh.magamGG.domain.project.dto.request.KanbanCardUpdateRequest;
import com.kh.magamGG.domain.project.dto.response.KanbanBoardResponse;
import com.kh.magamGG.domain.project.dto.response.KanbanCardResponse;
import com.kh.magamGG.domain.project.dto.response.CalendarCardResponse;
import com.kh.magamGG.domain.project.dto.response.TodayTaskResponse;

import java.util.List;

public interface KanbanBoardService {

    /** 회원별 칸반 카드 통계 (진행중 N, 완료 Y 개수) */
    MemberKanbanStatsResponseDto getKanbanStatsForMember(Long memberNo);

    /** 아티스트 대시보드 오늘 할 일: 담당자 배정 + 마감일 오늘 + 미완료(N) 카드만 */
    List<TodayTaskResponse> getTodayTasksForMember(Long memberNo);

    /** 아티스트 캘린더: 담당자 배정 카드 중 해당 월과 기간 겹치는 카드 (PROJECT_COLOR, STARTED_AT, ENDED_AT) */
    List<CalendarCardResponse> getCalendarCardsForMember(Long memberNo, int year, int month);

    /** 마감임박 업무: KANBAN_CARD_ENDED_AT >= 오늘, 이전 날짜 제외, 마감일 가까운 순 (최대 10건) */
    List<CalendarCardResponse> getDeadlineCardsForMember(Long memberNo);

    List<KanbanBoardResponse> getBoardsByProjectNo(Long projectNo);

    KanbanBoardResponse createBoard(Long projectNo, KanbanBoardCreateRequest request);

    void updateBoardStatus(Long projectNo, Long boardId, KanbanBoardUpdateRequest request);

    KanbanCardResponse createCard(Long projectNo, KanbanCardCreateRequest request);

    KanbanCardResponse updateCard(Long projectNo, Long cardId, KanbanCardUpdateRequest request);
}


