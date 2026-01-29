package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    
    /**
     * 특정 에이전시의 담당자 목록 조회
     */
    @Query("SELECT DISTINCT m FROM Manager m JOIN FETCH m.member mem LEFT JOIN FETCH mem.agency WHERE (mem.agency IS NOT NULL AND mem.agency.agencyNo = :agencyNo)")
    List<Manager> findByAgencyNo(@Param("agencyNo") Long agencyNo);
    
    /**
     * 멤버 번호로 담당자 조회
     */
    Optional<Manager> findByMember_MemberNo(Long memberNo);
}
