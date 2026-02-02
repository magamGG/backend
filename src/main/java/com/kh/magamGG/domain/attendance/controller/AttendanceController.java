package com.kh.magamGG.domain.attendance.controller;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;
import com.kh.magamGG.domain.attendance.dto.response.AttendanceStartResponse;
import com.kh.magamGG.domain.attendance.dto.response.TodayAttendanceStatusResponse;
import com.kh.magamGG.domain.attendance.service.AttendanceService;
import com.kh.magamGG.domain.health.dto.request.DailyHealthCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
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
	
	/**
	 * 출근 시작 (건강 체크 + 출근 기록)
	 * POST /api/attendance/start
	 * 
	 * @param healthCheckRequest 건강 체크 정보
	 * @param memberNo 회원 번호 (헤더에서 추출)
	 * @return 성공 여부
	 */
	@PostMapping("/start")
	public ResponseEntity<AttendanceStartResponse> startAttendance(
			@RequestBody DailyHealthCheckRequest healthCheckRequest,
			@RequestHeader("Member-No") Long memberNo) {
		
		log.info("출근 시작 요청: 회원번호={}", memberNo);
		
		boolean success = attendanceService.startAttendance(healthCheckRequest, memberNo);
		
		AttendanceStartResponse response = AttendanceStartResponse.builder()
				.success(success)
				.message(success ? "출근이 시작되었습니다." : "출근 시작에 실패했습니다.")
				.build();
		
		log.info("출근 시작 완료: 회원번호={}, 성공여부={}", memberNo, success);
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 오늘 날짜의 마지막 출근 상태 조회
	 * GET /api/attendance/today-status
	 * 
	 * @param memberNo 회원 번호 (헤더에서 추출)
	 * @return 마지막 출근 타입 ('출근' 또는 null)
	 */
	@GetMapping("/today-status")
	public ResponseEntity<TodayAttendanceStatusResponse> getTodayAttendanceStatus(
			@RequestHeader("Member-No") Long memberNo) {
		
		log.info("오늘 출근 상태 조회: 회원번호={}", memberNo);
		
		String lastAttendanceType = attendanceService.getTodayLastAttendanceType(memberNo);
		
		TodayAttendanceStatusResponse response = TodayAttendanceStatusResponse.builder()
				.lastAttendanceType(lastAttendanceType)
				.working("출근".equals(lastAttendanceType))
				.build();
		
		log.info("오늘 출근 상태 조회 완료: 회원번호={}, 마지막타입={}", memberNo, lastAttendanceType);
		
		return ResponseEntity.ok(response);
	}

	/**
	 * 출근 종료 (퇴근 기록)
	 * POST /api/attendance/end
	 */
	@PostMapping("/end")
	public ResponseEntity<AttendanceStartResponse> endAttendance(
			@RequestHeader("Member-No") Long memberNo) {
		log.info("출근 종료 요청: 회원번호={}", memberNo);
		boolean success = attendanceService.endAttendance(memberNo);
		AttendanceStartResponse response = AttendanceStartResponse.builder()
				.success(success)
				.message(success ? "출근이 종료되었습니다." : "출근 종료에 실패했습니다.")
				.build();
		return ResponseEntity.ok(response);
	}
}


