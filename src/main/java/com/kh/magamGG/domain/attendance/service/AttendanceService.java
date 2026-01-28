package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.dto.AttendanceStatisticsResponseDto;

public interface AttendanceService {
	
	/**
	 * 회원별 월별 근태 통계 조회
	 */
	AttendanceStatisticsResponseDto getAttendanceStatistics(Long memberNo, int year, int month);
}


