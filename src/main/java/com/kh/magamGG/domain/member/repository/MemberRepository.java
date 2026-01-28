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

	@Query("SELECT m.memberRole, COUNT(m) FROM Member m WHERE m.agency.agencyNo = :agencyNo GROUP BY m.memberRole")
	List<Object[]> countByAgencyNoAndMemberRole(@Param("agencyNo") Long agencyNo);
}
