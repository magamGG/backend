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
     * 에이전시 번호로 소속 회원 목록 조회
     */
    List<Member> findByAgency_AgencyNo(Long agencyNo);

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

    /**
     * 에이전시별 역할별 회원 수 조회 (마이페이지 통계용)
     */
    @Query("SELECT m.memberRole, COUNT(m) FROM Member m WHERE m.agency.agencyNo = :agencyNo GROUP BY m.memberRole")
    List<Object[]> countByAgencyNoAndMemberRole(@Param("agencyNo") Long agencyNo);

    /**
     * 에이전시별 담당자 목록 조회 (member_role = "담당자")
     * 주의: 이제 MANAGER 테이블을 통해 조회하므로 이 메서드는 사용하지 않음
     */
    @Deprecated
    @Query("SELECT DISTINCT m FROM Member m LEFT JOIN FETCH m.agency WHERE m.agency.agencyNo = :agencyNo AND m.memberRole = '담당자'")
    List<Member> findManagersByAgencyNo(@Param("agencyNo") Long agencyNo);

    /**
     * 에이전시별 작가 목록 조회 (member_role IN ("웹툰 작가", "웹소설 작가"))
     * manager는 필요할 때만 조회하도록 별도 처리
     */
    @Query("SELECT DISTINCT m FROM Member m LEFT JOIN FETCH m.agency WHERE m.agency.agencyNo = :agencyNo AND (m.memberRole = '웹툰 작가' OR m.memberRole = '웹소설 작가')")
    List<Member> findArtistsByAgencyNo(@Param("agencyNo") Long agencyNo);

    /**
     * 특정 담당자에게 배정된 작가 목록 조회
     * TODO: MANAGER_NO 컬럼 추가 후 주석 해제
     */
    // @Query("SELECT m FROM Member m WHERE m.manager.memberNo = :managerNo")
    // List<Member> findArtistsByManagerNo(@Param("managerNo") Long managerNo);
}
