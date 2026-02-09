package com.kh.magamGG.domain.calendar.service;

import com.kh.magamGG.domain.calendar.dto.response.DeadlineCountResponse;
import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CalendarServiceImpl implements CalendarService {

    private final KanbanCardRepository kanbanCardRepository;
    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final MemberRepository memberRepository;

    private static final String[] DAY_LABELS = {"오늘", "내일", "2일 후", "3일 후", "4일 후"};

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

        // 3. 담당 작가들의 칸반 카드 조회 (마감일이 오늘~4일 후인 것만)
        List<KanbanCard> cards = kanbanCardRepository.findByMemberNosAndDateRange(
                artistMemberNos, today, toDate);

        // 4. 날짜별 카운트 (오늘=0, 내일=1, 2일후=2, 3일후=3, 4일후=4)
        int[] counts = new int[5];
        for (KanbanCard card : cards) {
            LocalDate endedAt = card.getKanbanCardEndedAt();
            if (endedAt != null) {
                long daysDiff = ChronoUnit.DAYS.between(today, endedAt);
                if (daysDiff >= 0 && daysDiff <= 4) {
                    counts[(int) daysDiff]++;
                }
            }
        }

        // 5. 응답 생성
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
        // 1. 에이전시 소속 작가 목록 조회
        List<Member> artists = memberRepository.findArtistsByAgencyNo(agencyNo);
        List<Long> artistMemberNos = artists.stream()
                .map(Member::getMemberNo)
                .distinct()
                .collect(Collectors.toList());

        if (artistMemberNos.isEmpty()) {
            return buildEmptyDeadlineCounts();
        }

        // 2. 오늘 ~ 4일 후 기간
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(4);

        // 3. 에이전시 소속 작가들의 칸반 카드 조회 (마감일이 오늘~4일 후인 것만)
        List<KanbanCard> cards = kanbanCardRepository.findByMemberNosAndDateRange(
                artistMemberNos, today, toDate);

        // 4. 날짜별 카운트
        int[] counts = new int[5];
        for (KanbanCard card : cards) {
            LocalDate endedAt = card.getKanbanCardEndedAt();
            if (endedAt != null) {
                long daysDiff = ChronoUnit.DAYS.between(today, endedAt);
                if (daysDiff >= 0 && daysDiff <= 4) {
                    counts[(int) daysDiff]++;
                }
            }
        }

        // 5. 응답 생성
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

