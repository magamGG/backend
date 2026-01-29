package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.AttendanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, Long> {
    
    /**
     * 특정 회원의 근태 신청 목록을 최신순으로 조회
     */
    List<AttendanceRequest> findByMember_MemberNoOrderByAttendanceRequestCreatedAtDesc(Long memberNo);
    
    /**
     * 특정 상태의 근태 신청 목록 조회
     */
    List<AttendanceRequest> findByAttendanceRequestStatusOrderByAttendanceRequestCreatedAtDesc(String status);
    
    /**
     * 특정 에이전시 소속 회원들의 근태 신청 목록 조회 (N+1 방지를 위한 JOIN FETCH)
     * Member를 통해 Agency 번호 참조
     */
    @Query("SELECT ar FROM AttendanceRequest ar " +
           "JOIN FETCH ar.member m " +
           "WHERE m.agency.agencyNo = :agencyNo " +
           "ORDER BY ar.attendanceRequestCreatedAt DESC")
    List<AttendanceRequest> findByAgencyNoWithMember(@Param("agencyNo") Long agencyNo);
    
    /**
     * 특정 에이전시 소속 회원들의 대기 중인 근태 신청 목록 조회 (N+1 방지)
     */
    @Query("SELECT ar FROM AttendanceRequest ar " +
           "JOIN FETCH ar.member m " +
           "WHERE m.agency.agencyNo = :agencyNo " +
           "AND ar.attendanceRequestStatus = :status " +
           "ORDER BY ar.attendanceRequestCreatedAt DESC")
    List<AttendanceRequest> findByAgencyNoAndStatusWithMember(
            @Param("agencyNo") Long agencyNo, 
            @Param("status") String status);
    
    /**
     * 특정 회원의 승인된 근태 신청 조회 (날짜 필터링은 서비스 레이어에서 처리)
     * - ATTENDANCE_REQUEST_STATUS = 'APPROVED'
     */
    @Query("SELECT ar FROM AttendanceRequest ar " +
           "JOIN FETCH ar.member m " +
           "WHERE m.memberNo = :memberNo " +
           "AND ar.attendanceRequestStatus = 'APPROVED' " +
           "ORDER BY ar.attendanceRequestCreatedAt DESC")
    List<AttendanceRequest> findApprovedByMemberNo(@Param("memberNo") Long memberNo);
}


