package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.LeaveHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveHistoryRepository extends JpaRepository<LeaveHistory, Long> {

    /**
     * 에이전시 소속 회원들의 연차 변경 이력 조회 (최신순)
     */
    @Query("SELECT lh FROM LeaveHistory lh " +
           "JOIN FETCH lh.member m " +
           "WHERE m.agency.agencyNo = :agencyNo " +
           "ORDER BY lh.leaveHistoryDate DESC")
    List<LeaveHistory> findByAgencyNoWithMember(@Param("agencyNo") Long agencyNo);
}
