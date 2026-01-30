package com.kh.magamGG.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatisticsResponseDto {
	private List<RoleCount> roleCounts;
	private Integer totalCount;
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RoleCount {
		private String role;
		private Long count;
	}
}

