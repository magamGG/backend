package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
	
	/**
	 * 회원의 오늘 날짜의 마지막 출근 기록 조회
	 * @param memberNo 회원 번호
	 * @param today 오늘 날짜 (시간 제외)
	 * @return 마지막 출근 기록 (없으면 null)
	 */
	@Query("SELECT a FROM Attendance a " +
		   "WHERE a.member.memberNo = :memberNo " +
		   "AND DATE(a.attendanceTime) = :today " +
		   "ORDER BY a.attendanceTime DESC")
	List<Attendance> findTodayLastAttendanceByMemberNo(
		@Param("memberNo") Long memberNo,
		@Param("today") java.time.LocalDate today
	);

	/**
	 * 에이전시별 특정 날짜 출근(체크인)한 회원 번호 목록 조회
	 */
	@Query("SELECT DISTINCT a.member.memberNo FROM Attendance a " +
		   "WHERE a.agency.agencyNo = :agencyNo " +
		   "AND DATE(a.attendanceTime) = :date " +
		   "AND a.attendanceType = '출근'")
	List<Long> findMemberNosCheckedInByAgencyAndDate(
		@Param("agencyNo") Long agencyNo,
		@Param("date") LocalDate date
	);
}


