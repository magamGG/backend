package com.kh.magamGG.domain.calendar.service;

import com.kh.magamGG.domain.calendar.dto.response.CalendarEventResponse;
import com.kh.magamGG.domain.calendar.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import com.kh.magamGG.domain.calendar.repository.CalendarEventRepository;
import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final MemberRepository memberRepository;

    private static final String[] DAY_LABELS = {"오늘", "내일", "2일 후", "3일 후", "4일 후"};

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

    @Override
    public List<DeadlineCountResponse> getDeadlineCountsForManager(Long memberNo) {
        // 1. 담당자 조회 (memberNo -> managerNo)
        Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
        if (managerOpt.isEmpty()) {
            log.debug("담당자 아님 또는 미등록: memberNo={}", memberNo);
            return buildEmptyDeadlineCounts();
        }

        Manager manager = managerOpt.get();
        List<ArtistAssignment> assignments = artistAssignmentRepository.findByManagerNo(manager.getManagerNo());
        List<Long> artistMemberNos = assignments.stream()
                .map(a -> a.getArtist().getMemberNo())
                .distinct()
                .collect(Collectors.toList());

        if (artistMemberNos.isEmpty()) {
            return buildEmptyDeadlineCounts();
        }

        // 2. 오늘 ~ 4일 후 기간
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(4);

        List<CalendarEvent> events = calendarEventRepository.findByMemberNosAndDateRange(
                artistMemberNos, today, toDate);

        // 3. 날짜별 카운트 (오늘=0, 내일=1, 2일후=2, 3일후=3, 4일후=4)
        int[] counts = new int[5];
        for (CalendarEvent event : events) {
            LocalDate endedAt = event.getCalendarEventEndedAt();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, endedAt);
            if (daysDiff >= 0 && daysDiff <= 4) {
                counts[(int) daysDiff]++;
            }
        }

        // 4. 응답 생성
        List<DeadlineCountResponse> result = new ArrayList<>();
        for (int i = 0; i < DAY_LABELS.length; i++) {
            result.add(DeadlineCountResponse.builder()
                    .name(DAY_LABELS[i])
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    @Override
    public List<DeadlineCountResponse> getDeadlineCountsForAgency(Long agencyNo) {
        List<com.kh.magamGG.domain.member.entity.Member> artists =
                memberRepository.findArtistsByAgencyNo(agencyNo);
        List<Long> artistMemberNos = artists.stream()
                .map(com.kh.magamGG.domain.member.entity.Member::getMemberNo)
                .distinct()
                .collect(Collectors.toList());

        if (artistMemberNos.isEmpty()) {
            return buildEmptyDeadlineCounts();
        }

        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(4);

        List<CalendarEvent> events = calendarEventRepository.findByMemberNosAndDateRange(
                artistMemberNos, today, toDate);

        int[] counts = new int[5];
        for (CalendarEvent event : events) {
            LocalDate endedAt = event.getCalendarEventEndedAt();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, endedAt);
            if (daysDiff >= 0 && daysDiff <= 4) {
                counts[(int) daysDiff]++;
            }
        }

        List<DeadlineCountResponse> result = new ArrayList<>();
        for (int i = 0; i < DAY_LABELS.length; i++) {
            result.add(DeadlineCountResponse.builder()
                    .name(DAY_LABELS[i])
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    private List<DeadlineCountResponse> buildEmptyDeadlineCounts() {
        return Arrays.stream(DAY_LABELS)
                .map(label -> DeadlineCountResponse.builder()
                        .name(label)
                        .count(0)
                        .build())
                .collect(Collectors.toList());
    }
}