package com.kh.magamGG.domain.calendar.service;

import com.kh.magamGG.domain.calendar.dto.response.CalendarEventResponse;

import java.util.List;

/**
 * 캘린더 이벤트 서비스
 */
public interface CalendarEventService {

    /**
     * 특정 회원의 특정 연월 일정 조회
     * @param memberNo 회원 번호
     * @param year 연도
     * @param month 월 (1-12)
     */
    List<CalendarEventResponse> getEventsByMonth(Long memberNo, int year, int month);

    /**
     * 특정 회원의 다가오는 일정 조회 (작가 대시보드 "다음 연재 프로젝트"용)
     * @param memberNo 회원 번호
     * @param limit 최대 건수
     */
    List<CalendarEventResponse> getUpcomingEvents(Long memberNo, int limit);
}