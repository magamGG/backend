package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ARTIST_ASSIGNMENT: MANAGER_NO(담당자)와 MEMBER_NO(작가) 중계 테이블 조회.
 * 배정된 작가 = ARTIST_ASSIGNMENT에 (MANAGER_NO = 담당자, ARTIST_MEMBER_NO = 작가 MEMBER_NO) 행이 있는 경우.
 */
public interface ArtistAssignmentRepository extends JpaRepository<ArtistAssignment, Long> {

    /** 특정 작가(artistMemberNo = MEMBER_NO)의 배정 조회 — ARTIST_ASSIGNMENT에 해당 MEMBER_NO가 있는지 */
    @Query("SELECT a FROM ArtistAssignment a JOIN FETCH a.manager JOIN FETCH a.artist WHERE a.artist.memberNo = :artistMemberNo")
    Optional<ArtistAssignment> findByArtistMemberNo(@Param("artistMemberNo") Long artistMemberNo);

    /** 특정 담당자(MANAGER_NO)에게 배정된 작가 목록 — ARTIST_ASSIGNMENT.MANAGER_NO = :managerNo 인 행의 작가들 */
    @Query("SELECT a FROM ArtistAssignment a JOIN FETCH a.artist WHERE a.manager.managerNo = :managerNo")
    List<ArtistAssignment> findByManagerNo(@Param("managerNo") Long managerNo);

    /** 배정 여부: 해당 MANAGER_NO와 해당 작가 MEMBER_NO 조합이 ARTIST_ASSIGNMENT에 존재하는지 */
    boolean existsByManager_ManagerNoAndArtist_MemberNo(Long managerNo, Long artistMemberNo);

    /**
     * 특정 작가의 배정 삭제
     */
    void deleteByArtist_MemberNo(Long artistMemberNo);
}
