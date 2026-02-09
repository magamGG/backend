package com.kh.magamGG.domain.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오늘 출근 상태 조회 API 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceStatusResponse {
	/** 오늘 날짜의 마지막 출근 타입 ('출근', '퇴근' 등, 없으면 null) */
	private String lastAttendanceType;
	/** 현재 출근 중 여부 (마지막 타입이 '출근'이면 true) */
	@JsonProperty("isWorking")
	private boolean working;
}
