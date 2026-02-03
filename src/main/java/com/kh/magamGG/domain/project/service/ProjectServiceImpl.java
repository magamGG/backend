package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.member.entity.Manager;
import com.kh.magamGG.domain.member.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.member.repository.ManagerRepository;
import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.ProjectMember;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectMemberRepository;
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
public class ProjectServiceImpl implements ProjectService {

    private final ManagerRepository managerRepository;
    private final ArtistAssignmentRepository artistAssignmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final KanbanCardRepository kanbanCardRepository;

    private static final int DEADLINE_WARNING_DAYS = 7;
    private static final int PROGRESS_WARNING_THRESHOLD = 70;

    @Override
    public List<ManagedProjectResponse> getManagedProjectsByManager(Long memberNo) {
        Optional<Manager> managerOpt = managerRepository.findByMember_MemberNo(memberNo);
        if (managerOpt.isEmpty()) {
            log.debug("담당자 아님: memberNo={}", memberNo);
            return List.of();
        }

        Set<Long> artistMemberNos = artistAssignmentRepository.findByManagerNo(managerOpt.get().getManagerNo())
                .stream()
                .map(a -> a.getArtist().getMemberNo())
                .collect(Collectors.toSet());

        if (artistMemberNos.isEmpty()) {
            return List.of();
        }

        List<ProjectMember> allProjectMembers = new ArrayList<>();
        for (Long artistNo : artistMemberNos) {
            allProjectMembers.addAll(projectMemberRepository.findByMember_MemberNo(artistNo));
        }

        Set<Long> seenProjectNos = new HashSet<>();
        List<ManagedProjectResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (ProjectMember pm : allProjectMembers) {
            var project = pm.getProject();
            if (seenProjectNos.contains(project.getProjectNo())) {
                continue;
            }
            seenProjectNos.add(project.getProjectNo());

            List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(project.getProjectNo());
            int total = cards.size();
            int completed = (int) cards.stream()
                    .filter(c -> "Y".equals(c.getKanbanCardStatus()))
                    .count();
            int progress = total > 0 ? (completed * 100 / total) : 0;

            Optional<LocalDate> nearestDeadline = cards.stream()
                    .map(KanbanCard::getKanbanCardEndedAt)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isBefore(today))
                    .min(LocalDate::compareTo);
            if (nearestDeadline.isEmpty()) {
                nearestDeadline = cards.stream()
                        .map(KanbanCard::getKanbanCardEndedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo);
            }

            String deadlineStr = nearestDeadline
                    .map(d -> d.getMonthValue() + "월 " + d.getDayOfMonth() + "일")
                    .orElse("-");

            long daysUntilDeadline = nearestDeadline
                    .map(d -> ChronoUnit.DAYS.between(today, d))
                    .orElse(999L);
            boolean isDeadlineSoon = daysUntilDeadline >= 0 && daysUntilDeadline <= DEADLINE_WARNING_DAYS;
            String status = (isDeadlineSoon && progress < PROGRESS_WARNING_THRESHOLD) ? "주의" : "정상";

            String artistName = pm.getMember().getMemberName();

            result.add(ManagedProjectResponse.builder()
                    .projectNo(project.getProjectNo())
                    .projectName(project.getProjectName())
                    .artist(artistName)
                    .status(status)
                    .progress(progress)
                    .deadline(deadlineStr)
                    .build());
        }

        result.sort(Comparator.comparing(ManagedProjectResponse::getProjectNo));
        return result;
    }
}


