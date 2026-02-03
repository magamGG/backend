package com.kh.magamGG.domain.calendar.repository;

import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * 특정 회원의 특정 월에 겹치는 일정 조회
     * (시작일이 월 내이거나, 종료일이 월 내이거나, 월을 포함하는 기간)
     */
    @Query("SELECT ce FROM CalendarEvent ce " +
           "JOIN FETCH ce.member m " +
           "WHERE m.memberNo = :memberNo " +
           "AND ce.calendarEventStartedAt <= :monthEnd " +
           "AND ce.calendarEventEndedAt >= :monthStart " +
           "ORDER BY ce.calendarEventEndedAt ASC")
    List<CalendarEvent> findByMemberNoAndDateRange(
            @Param("memberNo") Long memberNo,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);    /**
     * 특정 회원의 다가오는 일정 조회 (마감일 기준, 오늘 이후)
     * 작가 대시보드 "다음 연재 프로젝트"용
     */
    @Query("SELECT ce FROM CalendarEvent ce " +
           "JOIN FETCH ce.member m " +
           "WHERE m.memberNo = :memberNo " +
           "AND ce.calendarEventEndedAt >= :fromDate " +
           "ORDER BY ce.calendarEventEndedAt ASC")
    List<CalendarEvent> findUpcomingByMemberNo(
            @Param("memberNo") Long memberNo,
            @Param("fromDate") LocalDate fromDate,
            Pageable pageable);

    /**
     * 여러 회원의 특정 기간 내 마감 일정 조회 (담당자 대시보드 마감 임박 현황용)
     * memberNo IN (...)
     * AND calendarEventEndedAt BETWEEN :fromDate AND :toDate
     */
    @Query("SELECT ce FROM CalendarEvent ce " +
           "JOIN FETCH ce.member m " +
           "WHERE m.memberNo IN :memberNos " +
           "AND ce.calendarEventEndedAt >= :fromDate " +
           "AND ce.calendarEventEndedAt <= :toDate " +
           "ORDER BY ce.calendarEventEndedAt ASC")
    List<CalendarEvent> findByMemberNosAndDateRange(
            @Param("memberNos") List<Long> memberNos,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
