package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.NewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewRequestRepository extends JpaRepository<NewRequest, Long> {
    List<NewRequest> findByAgency_AgencyNoOrderByNewRequestDateDesc(Long agencyNo);
    List<NewRequest> findByMember_MemberNo(Long memberNo);

    /** 해당 에이전시에서 승인된 가입 요청 수 (newRequestDate 기준 누적, 전월 대비 집계용) */
    long countByAgency_AgencyNoAndNewRequestStatusAndNewRequestDateLessThanEqual(
            Long agencyNo, String newRequestStatus, LocalDateTime dateTime);
}
