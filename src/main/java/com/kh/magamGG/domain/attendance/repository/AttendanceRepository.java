package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
	
	/**
	 * 회원별 월별 근태 타입별 통계 조회
	 */
	@Query("SELECT a.attendanceType, COUNT(a) FROM Attendance a " +
		   "WHERE a.member.memberNo = :memberNo " +
		   "AND YEAR(a.attendanceTime) = :year " +
		   "AND MONTH(a.attendanceTime) = :month " +
		   "GROUP BY a.attendanceType")
	List<Object[]> countByMemberNoAndMonth(
		@Param("memberNo") Long memberNo,
		@Param("year") int year,
		@Param("month") int month
	);
}


