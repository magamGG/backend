package com.kh.magamGG.domain.calendar.dto.response;

import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 캘린더 이벤트 응답 DTO
 * DB: CALENDAR_EVENT 테이블
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponse {

    private Long calendarEventNo;
    private Long memberNo;
    private String calendarEventName;
    private String calendarEventContent;
    private String calendarEventType;
    private LocalDate calendarEventStartedAt;
    private LocalDate calendarEventEndedAt;
    private LocalDateTime calendarEventCreatedAt;
    private LocalDateTime calendarEventUpdatedAt;
    private String calendarEventColor;

    public static CalendarEventResponse fromEntity(CalendarEvent entity) {
        return CalendarEventResponse.builder()
                .calendarEventNo(entity.getCalendarEventNo())
                .memberNo(entity.getMember().getMemberNo())
                .calendarEventName(entity.getCalendarEventName())
                .calendarEventContent(entity.getCalendarEventContent())
                .calendarEventType(entity.getCalendarEventType())
                .calendarEventStartedAt(entity.getCalendarEventStartedAt())
                .calendarEventEndedAt(entity.getCalendarEventEndedAt())
                .calendarEventCreatedAt(entity.getCalendarEventCreatedAt())
                .calendarEventUpdatedAt(entity.getCalendarEventUpdatedAt())
                .calendarEventColor(entity.getCalendarEventColor())
                .build();
    }
}
