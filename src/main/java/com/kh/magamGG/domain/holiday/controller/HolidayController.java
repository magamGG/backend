package com.kh.magamGG.domain.holiday.controller;

import com.kh.magamGG.domain.holiday.dto.response.HolidayResponse;
import com.kh.magamGG.domain.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {
    
    private final HolidayService holidayService;
    
    @GetMapping("/{year}")
    public ResponseEntity<HolidayResponse> getHolidays(@PathVariable int year) {
        HolidayResponse response = holidayService.getHolidaysByYear(year);
        return ResponseEntity.ok(response);
    }
}

