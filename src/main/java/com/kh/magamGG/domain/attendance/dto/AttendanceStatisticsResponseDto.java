package com.kh.magamGG.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatisticsResponseDto {
	private List<TypeCount> typeCounts;
	private Integer totalCount;
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TypeCount {
		private String type;
		private Long count;
	}
}

