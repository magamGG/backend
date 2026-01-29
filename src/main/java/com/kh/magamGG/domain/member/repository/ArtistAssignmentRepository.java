package com.kh.magamGG.domain.member.repository;

import com.kh.magamGG.domain.member.entity.ArtistAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistAssignmentRepository extends JpaRepository<ArtistAssignment, Long> {
    
    /**
     * 특정 작가의 배정 조회
     */
    @Query("SELECT a FROM ArtistAssignment a JOIN FETCH a.manager JOIN FETCH a.artist WHERE a.artist.memberNo = :artistMemberNo")
    Optional<ArtistAssignment> findByArtistMemberNo(@Param("artistMemberNo") Long artistMemberNo);
    
    /**
     * 특정 담당자에게 배정된 작가 목록 조회
     */
    @Query("SELECT a FROM ArtistAssignment a JOIN FETCH a.artist WHERE a.manager.managerNo = :managerNo")
    List<ArtistAssignment> findByManagerNo(@Param("managerNo") Long managerNo);
    
    /**
     * 특정 작가의 배정 삭제
     */
    void deleteByArtist_MemberNo(Long artistMemberNo);
}
