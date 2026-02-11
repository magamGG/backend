package com.kh.magamGG.domain.health.service;

import com.kh.magamGG.domain.health.entity.DailyHealthCheck;
import com.kh.magamGG.domain.health.repository.DailyHealthCheckRepository;
import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 3일 연속 건강 위험 감지 시 담당자/에이전시 관리자에게 알림 발송.
 * 동일 수신자에게 중복 알림 방지: 여러 명이 해당되면 한 건으로 통합.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HealthRiskNotificationService {

    private static final String CONDITION_TIRED = "피곤함";
    private static final int MAX_SLEEP_HOURS = 4;
    private static final int MIN_DISCOMFORT_LEVEL = 8;
    private static final int CONSECUTIVE_DAYS = 3;

    private final DailyHealthCheckRepository dailyHealthCheckRepository;
    private final MemberRepository memberRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final NotificationService notificationService;

    /**
     * 건강 체크 저장 후 호출. 3일 연속 위험 조건 충족 시 알림 발송 (동일 수신자 통합)
     *
     * @param memberNo 방금 건강 체크를 등록한 회원 번호
     */
    @Transactional
    public void checkAndNotifyIfRisk(Long memberNo) {
        Member member = memberRepository.findByIdWithAgency(memberNo).orElse(null);
        if (member == null || member.getAgency() == null) return;

        Long agencyNo = member.getAgency().getAgencyNo();
        if (!hasThreeConsecutiveRiskDays(memberNo)) return;

        // 해당 에이전시 소속 회원 중 3일 연속 위험 조건 충족자 수집
        List<Member> agencyMembers = memberRepository.findByAgency_AgencyNo(agencyNo);
        List<Member> atRiskMembers = agencyMembers.stream()
                .filter(m -> !"에이전시 관리자".equals(m.getMemberRole()))
                .filter(m -> hasThreeConsecutiveRiskDays(m.getMemberNo()))
                .collect(Collectors.toList());

        if (atRiskMembers.isEmpty()) return;

        // 수신자별 회원명 리스트 (중복 알림 방지)
        Map<Long, List<String>> recipientToNames = new HashMap<>();

        for (Member atRisk : atRiskMembers) {
            Long recipientNo = resolveRecipient(atRisk);
            if (recipientNo == null) continue;
            recipientToNames
                    .computeIfAbsent(recipientNo, k -> new ArrayList<>())
                    .add(atRisk.getMemberName());
        }

        // 수신자별로 통합 알림 1건 발송
        for (Map.Entry<Long, List<String>> e : recipientToNames.entrySet()) {
            String names = String.join(", ", e.getValue());
            String message = names + "님의 컨디션이 3일 연속 위험 수준입니다. 확인해 주세요.";
            notificationService.createNotification(e.getKey(), "건강 위험 감지", message, "HEALTH_WARN");
        }
    }

    /** 메인 작가 → 담당자, 없으면 에이전시 관리자 / 어시스트·담당자 → 에이전시 관리자 */
    private Long resolveRecipient(Member member) {
        String role = member.getMemberRole();
        boolean isMainArtist = "웹툰 작가".equals(role) || "웹소설 작가".equals(role);
        Long agencyNo = member.getAgency() != null ? member.getAgency().getAgencyNo() : null;
        if (agencyNo == null) return null;

        if (isMainArtist) {
            Optional<ArtistAssignment> assignment = artistAssignmentRepository.findByArtistMemberNo(member.getMemberNo());
            if (assignment.isPresent()) {
                return assignment.get().getManager().getMember().getMemberNo();
            }
        }

        // 담당자가 없거나 어시스트/담당자 역할 → 에이전시 관리자에게 (여러 명일 수 있음)
        List<Member> admins = memberRepository.findByAgency_AgencyNoAndMemberRoleIn(agencyNo, List.of("에이전시 관리자"));
        return admins.isEmpty() ? null : admins.get(0).getMemberNo(); // 첫 번째 관리자 (통합 알림은 1명만)
    }

    /** 최근 3일 연속 피곤함 + 수면≤4h + 불편≥8 조건 충족 여부 */
    private boolean hasThreeConsecutiveRiskDays(Long memberNo) {
        List<DailyHealthCheck> recent = dailyHealthCheckRepository
                .findTop10ByMember_MemberNoOrderByHealthCheckCreatedAtDesc(memberNo);
        if (recent.size() < CONSECUTIVE_DAYS) return false;

        List<LocalDate> dates = recent.stream()
                .map(d -> d.getHealthCheckCreatedAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(CONSECUTIVE_DAYS)
                .collect(Collectors.toList());
        if (dates.size() < CONSECUTIVE_DAYS) return false;

        for (int i = 0; i < dates.size() - 1; i++) {
            if (!dates.get(i).minusDays(1).equals(dates.get(i + 1))) return false;
        }

        Set<LocalDate> targetDates = new HashSet<>(dates);
        long matchCount = recent.stream()
                .filter(d -> targetDates.contains(d.getHealthCheckCreatedAt().toLocalDate()))
                .filter(this::meetsRiskCondition)
                .map(d -> d.getHealthCheckCreatedAt().toLocalDate())
                .distinct()
                .count();
        return matchCount >= CONSECUTIVE_DAYS;
    }

    private boolean meetsRiskCondition(DailyHealthCheck d) {
        if (!CONDITION_TIRED.equals(d.getHealthCondition())) return false;
        if (d.getSleepHours() == null || d.getSleepHours() > MAX_SLEEP_HOURS) return false;
        int discomfort = d.getDiscomfortLevel() != null ? d.getDiscomfortLevel() : 0;
        return discomfort >= MIN_DISCOMFORT_LEVEL;
    }
}
