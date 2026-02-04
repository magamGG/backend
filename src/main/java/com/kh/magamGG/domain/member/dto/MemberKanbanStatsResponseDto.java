package com.kh.magamGG.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원별 칸반 카드(KANBAN_CARD) 통계 응답
 * - totalCount: 전체 작업 개수 (진행중 + 완료, status Y/N)
 * - inProgressCount: 진행중인 작업 (kanbanCardStatus = 'N')
 * - completedCount: 완료된 작업 (kanbanCardStatus = 'Y')
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberKanbanStatsResponseDto {
    private int totalCount;
    private int inProgressCount;
    private int completedCount;
}
