package com.kh.magamGG.domain.calendar.controller;

import com.kh.magamGG.domain.calendar.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 캘린더 API
 * 칸반 카드 마감일 기반 일정 관리
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 담당자 대시보드 마감 임박 현황 (오늘~4일 후별 건수)
     * 칸반 카드 마감일 기준
     * GET /api/calendar/deadline-counts
     */
    @GetMapping("/deadline-counts")
    public ResponseEntity<List<DeadlineCountResponse>> getDeadlineCounts(
            @RequestHeader("X-Member-No") Long memberNo) {

        log.info("마감 임박 현황 조회: 담당자 회원={}", memberNo);

        List<DeadlineCountResponse> counts = calendarService.getDeadlineCountsForManager(memberNo);
        return ResponseEntity.ok(counts);
    }

    /**
     * 에이전시 대시보드 마감 임박 현황 (오늘~4일 후별 건수)
     * 칸반 카드 마감일 기준
     * GET /api/calendar/deadline-counts/agency/{agencyNo}
     */
    @GetMapping("/deadline-counts/agency/{agencyNo}")
    public ResponseEntity<List<DeadlineCountResponse>> getDeadlineCountsByAgency(
            @PathVariable Long agencyNo) {

        log.info("마감 임박 현황 조회: 에이전시={}", agencyNo);

        List<DeadlineCountResponse> counts = calendarService.getDeadlineCountsForAgency(agencyNo);
        return ResponseEntity.ok(counts);
    }
}
