package com.kh.magamGG.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatisticsResponseDto {
	private List<TypeCount> typeCounts;
	private Integer totalCount;
	private List<CalendarEvent> calendarEvents; // 캘린더용 이벤트 데이터 추가
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TypeCount {
		private String type;
		private Long count;
	}
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CalendarEvent {
		private Long memberNo;
		private String memberName;
		private LocalDate eventDate;
		private String eventType; // REMOTE_WORK, VACATION, WORKATION
		private String colorCode; // #FF8C00(주황), #808080(회색), #8A2BE2(보라)
		private String title;
	}
}

