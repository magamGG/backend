package com.kh.magamGG.domain.calendar.controller;

import com.kh.magamGG.domain.calendar.dto.response.DeadlineCountItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    /**
     * 에이전시 대시보드 마감 임박 현황 (오늘~4일 후별 건수)
     * GET /api/calendar/deadline-counts/agency/{agencyNo}
     * 데이터 없을 때 빈 목록 반환 (500 방지)
     */
    @GetMapping("/deadline-counts/agency/{agencyNo}")
    public ResponseEntity<List<DeadlineCountItemResponse>> getDeadlineCountsByAgency(@PathVariable Long agencyNo) {
        List<DeadlineCountItemResponse> list = Collections.emptyList();
        return ResponseEntity.ok(list);
    }
}
