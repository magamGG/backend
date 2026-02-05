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
}

