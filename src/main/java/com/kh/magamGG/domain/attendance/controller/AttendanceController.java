package com.kh.magamGG.domain.attendance.controller;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
	
	private final AttendanceService attendanceService;
	
	/**
	 * 회원별 월별 근태 통계 조회
	 */
	@GetMapping("/statistics/{memberNo}")
	public ResponseEntity<AttendanceStatisticsResponseDto> getAttendanceStatistics(
		@PathVariable Long memberNo,
		@RequestParam(required = false) Integer year,
		@RequestParam(required = false) Integer month
	) {
		// year와 month가 없으면 현재 년월 사용
		int currentYear = year != null ? year : java.time.LocalDate.now().getYear();
		int currentMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();
		
		AttendanceStatisticsResponseDto response = attendanceService.getAttendanceStatistics(
			memberNo, currentYear, currentMonth
		);
		
		return ResponseEntity.ok(response);
	}
}

