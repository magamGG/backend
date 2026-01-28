package com.kh.magamGG.domain.manager.service;

import com.kh.magamGG.domain.manager.entity.ArtistAssignment;
import com.kh.magamGG.domain.manager.entity.Manager;
import com.kh.magamGG.domain.manager.repository.ArtistAssignmentRepository;
import com.kh.magamGG.domain.manager.repository.ManagerRepository;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistAssignmentService {

    private final ArtistAssignmentRepository assignmentRepository;
    private final ManagerRepository managerRepository;
    private final MemberRepository memberRepository;

    public void assignArtist(Long managerNo, Long artistMemberNo) {

        if (assignmentRepository
                .existsByManager_ManagerNoAndArtist_MemberNo(
                        managerNo, artistMemberNo)) {
            throw new IllegalStateException("이미 배정된 작가입니다.");
        }

        Manager manager = managerRepository.findById(managerNo).orElseThrow();
        Member artist = memberRepository.findById(artistMemberNo).orElseThrow();

        ArtistAssignment assignment = ArtistAssignment.builder()
                .manager(manager)
                .artist(artist)
                .build();

        assignmentRepository.save(assignment);
    }

    public List<ArtistAssignment> getAssignedArtists(Long managerNo) {
        return assignmentRepository.findByManager_ManagerNo(managerNo);
    }
}
