package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberEmail(String memberEmail);
    boolean existsByMemberEmail(String memberEmail);
    
    /**
     * 특정 에이전시 소속이면서 특정 역할들 중 하나인 회원 목록 조회
     * (담당자/매니저에게 알림 보낼 때 사용)
     */
    List<Member> findByAgency_AgencyNoAndMemberRoleIn(Long agencyNo, List<String> roles);
    
    /**
     * 회원 조회 시 Agency 정보도 함께 조회 (N+1 방지)
     */
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.agency WHERE m.memberNo = :memberNo")
    Optional<Member> findByIdWithAgency(@Param("memberNo") Long memberNo);
}
