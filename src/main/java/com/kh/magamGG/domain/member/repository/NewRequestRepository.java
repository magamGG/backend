package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.NewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewRequestRepository extends JpaRepository<NewRequest, Long> {
    List<NewRequest> findByAgency_AgencyNoOrderByNewRequestDateDesc(Long agencyNo);
    List<NewRequest> findByMember_MemberNo(Long memberNo);
}
