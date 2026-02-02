package com.kh.magamGG.domain.calendar.controller;

import com.kh.magamGG.domain.calendar.dto.response.CalendarEventResponse;
import com.kh.magamGG.domain.calendar.service.CalendarEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 캘린더 이벤트 컨트롤러
 * 작가 대시보드 "다음 연재 프로젝트" 등 일정 조회 API
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    /**
     * 월별 일정 조회
     * GET /api/calendar/events?year=2026&month=1
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getEventsByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestHeader("X-Member-No") Long memberNo) {

        log.info("캘린더 일정 조회: 회원={}, {}-{}", memberNo, year, month);

        List<CalendarEventResponse> events = calendarEventService.getEventsByMonth(memberNo, year, month);
        return ResponseEntity.ok(events);
    }

    /**
     * 다가오는 일정 조회 (작가 대시보드 "다음 연재 프로젝트"용)
     * GET /api/calendar/events/upcoming?limit=10
     */
    @GetMapping("/events/upcoming")
    public ResponseEntity<List<CalendarEventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("X-Member-No") Long memberNo) {

        log.info("다가오는 일정 조회: 회원={}, limit={}", memberNo, limit);

        List<CalendarEventResponse> events = calendarEventService.getUpcomingEvents(memberNo, limit);
        return ResponseEntity.ok(events);
    }
}
