package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	/** 회원·연·월 기준 출근(체크인)한 날짜 목록 (날짜별 1건, 같은 날 여러 번 출근해도 1일) - 출근만 */
	@Query(value = "SELECT DISTINCT DATE(a.attendance_time) FROM attendance a " +
			"WHERE a.member_no = :memberNo AND YEAR(a.attendance_time) = :year AND MONTH(a.attendance_time) = :month " +
			"AND a.attendance_type = '출근'", nativeQuery = true)
	List<java.sql.Date> findDistinctCheckInDatesByMemberNoAndMonth(
			@Param("memberNo") Long memberNo,
			@Param("year") int year,
			@Param("month") int month);

	/** 회원·연·월 기준 근무일별 타입 (출근/재택근무/워케이션) - 집계용, 날짜별 1타입 */
	@Query(value = "SELECT DISTINCT DATE(a.attendance_time), a.attendance_type FROM attendance a " +
			"WHERE a.member_no = :memberNo AND YEAR(a.attendance_time) = :year AND MONTH(a.attendance_time) = :month " +
			"AND a.attendance_type IN ('출근','재택근무','워케이션')", nativeQuery = true)
	List<Object[]> findDistinctCheckInDatesWithTypeByMemberNoAndMonth(
			@Param("memberNo") Long memberNo,
			@Param("year") int year,
			@Param("month") int month);

	/** 에이전시 금일 출석 기록 조회 (금일 출석 분포용) */
	@Query("SELECT a FROM Attendance a WHERE a.agency.agencyNo = :agencyNo AND DATE(a.attendanceTime) = :today ORDER BY a.member.memberNo, a.attendanceTime DESC")
	List<Attendance> findByAgency_AgencyNoAndDate(@Param("agencyNo") Long agencyNo, @Param("today") LocalDate today);
	
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
	 * 회원별 월별 출근한 날 수 (날짜별 1회 집계, 같은 날 여러 출근/퇴근 있어도 1일로 카운트)
	 * 마이페이지 근태 통계용 - '출근'만 표시
	 */
	@Query("SELECT COUNT(DISTINCT FUNCTION('DATE', a.attendanceTime)) FROM Attendance a " +
		   "WHERE a.member.memberNo = :memberNo " +
		   "AND YEAR(a.attendanceTime) = :year " +
		   "AND MONTH(a.attendanceTime) = :month " +
		   "AND a.attendanceType = '출근'")
	long countDistinctCheckInDaysByMemberNoAndMonth(
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


