package com.kh.magamGG.domain.calendar.service;

import com.kh.magamGG.domain.calendar.dto.response.CalendarEventResponse;
import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import com.kh.magamGG.domain.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public List<CalendarEventResponse> getEventsByMonth(Long memberNo, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<CalendarEvent> events = calendarEventRepository.findByMemberNoAndDateRange(
                memberNo, monthStart, monthEnd);

        return events.stream()
                .map(CalendarEventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalendarEventResponse> getUpcomingEvents(Long memberNo, int limit) {
        LocalDate today = LocalDate.now();
        List<CalendarEvent> events = calendarEventRepository.findUpcomingByMemberNo(
                memberNo, today, PageRequest.of(0, limit));

        return events.stream()
                .map(CalendarEventResponse::fromEntity)
                .collect(Collectors.toList());
    }
}