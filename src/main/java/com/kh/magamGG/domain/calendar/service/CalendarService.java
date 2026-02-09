package com.kh.magamGG.domain.calendar.service;

import com.kh.magamGG.domain.calendar.dto.response.DeadlineCountResponse;

import java.util.List;

/**
 * 캘린더 서비스
 * 칸반 카드 마감일 기반 일정 관리
 */
public interface CalendarService {

    /**
     * 담당자의 담당 작가들 칸반 카드 기준 마감 임박 현황 (오늘~4일 후별 건수)
     * @param memberNo 로그인한 담당자 회원 번호 (X-Member-No)
     */
    List<DeadlineCountResponse> getDeadlineCountsForManager(Long memberNo);

    /**
     * 에이전시 소속 작가들 칸반 카드 기준 마감 임박 현황 (오늘~4일 후별 건수)
     * @param agencyNo 에이전시 번호
     */
    List<DeadlineCountResponse> getDeadlineCountsForAgency(Long agencyNo);
}

