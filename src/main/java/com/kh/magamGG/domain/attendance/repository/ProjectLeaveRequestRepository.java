package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.ProjectLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectLeaveRequestRepository extends JpaRepository<ProjectLeaveRequest, Long> {

    /**
     * 특정 근태 신청에 연결된 프로젝트 휴재 신청 조회 (1:0..1 관계)
     * @param attendanceRequestNo 근태 신청 번호
     * @return 프로젝트 휴재 신청 (없을 수 있음)
     */
    @Query("SELECT plr FROM ProjectLeaveRequest plr " +
           "JOIN FETCH plr.project " +
           "WHERE plr.attendanceRequest.attendanceRequestNo = :attendanceRequestNo")
    Optional<ProjectLeaveRequest> findByAttendanceRequestNo(@Param("attendanceRequestNo") Long attendanceRequestNo);

    /**
     * 특정 프로젝트의 휴재 신청 목록 조회 (1:N 관계)
     * @param projectNo 프로젝트 번호
     * @return 프로젝트 휴재 신청 목록
     */
    @Query("SELECT plr FROM ProjectLeaveRequest plr " +
           "JOIN FETCH plr.attendanceRequest ar " +
           "JOIN FETCH ar.member " +
           "WHERE plr.project.projectNo = :projectNo " +
           "ORDER BY ar.attendanceRequestCreatedAt DESC")
    java.util.List<ProjectLeaveRequest> findByProjectNo(@Param("projectNo") Long projectNo);
    
    /**
     * 특정 프로젝트의 특정 기간과 겹치는 휴재 신청 조회 (대기중 또는 승인된 것만)
     * 날짜 겹침 조건: 기존 신청의 시작일 <= 새 신청의 종료일 AND 기존 신청의 종료일 >= 새 신청의 시작일
     * @param projectNo 프로젝트 번호
     * @param startDate 신청 시작일
     * @param endDate 신청 종료일
     * @return 겹치는 휴재 신청 목록
     */
    @Query("SELECT plr FROM ProjectLeaveRequest plr " +
           "JOIN FETCH plr.attendanceRequest ar " +
           "WHERE plr.project.projectNo = :projectNo " +
           "AND ar.attendanceRequestStatus IN ('PENDING', 'APPROVED') " +
           "AND ar.attendanceRequestStartDate <= :endDate " +
           "AND ar.attendanceRequestEndDate >= :startDate")
    java.util.List<ProjectLeaveRequest> findOverlappingRequests(
            @Param("projectNo") Long projectNo,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 특정 프로젝트에 대한 승인된 휴재 신청 목록 (ATTENDANCE_REQUEST_TYPE=휴재, ATTENDANCE_REQUEST_STATUS=APPROVED)
     * 다음 연재일 계산 시 휴재 기간 스킵에 사용
     */
    @Query("SELECT plr FROM ProjectLeaveRequest plr " +
           "JOIN FETCH plr.attendanceRequest ar " +
           "WHERE plr.project.projectNo = :projectNo " +
           "AND ar.attendanceRequestType = '휴재' " +
           "AND ar.attendanceRequestStatus = 'APPROVED' " +
           "ORDER BY ar.attendanceRequestStartDate ASC")
    java.util.List<ProjectLeaveRequest> findApprovedHiatusByProjectNo(@Param("projectNo") Long projectNo);
}

