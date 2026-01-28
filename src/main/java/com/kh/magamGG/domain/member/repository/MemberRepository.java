package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberEmail(String memberEmail);
    boolean existsByMemberEmail(String memberEmail);
    List<Member> findByAgency_AgencyNo(Long agencyNo);
}
