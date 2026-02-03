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

    /**
     * 에이전시·연도별 회원 연차 조정 합계 (leave_history.leaveHistoryAmount 합계)
     * 직원 연차 관리 '조정' 컬럼용
     */
    @Query("SELECT lh.member.memberNo, COALESCE(SUM(lh.leaveHistoryAmount), 0) FROM LeaveHistory lh " +
           "WHERE lh.member.agency.agencyNo = :agencyNo AND FUNCTION('YEAR', lh.leaveHistoryDate) = :year " +
           "GROUP BY lh.member.memberNo")
    List<Object[]> sumAdjustmentByAgencyNoAndYear(@Param("agencyNo") Long agencyNo, @Param("year") int year);
}
