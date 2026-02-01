package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByMember_MemberNoOrderByLeaveBalanceYearDesc(Long memberNo);

    /**
     * 회원별 최신 연도 연차 잔액 1건만 조회 (memberNo 중복 제거)
     */
    Optional<LeaveBalance> findTop1ByMember_MemberNoOrderByLeaveBalanceYearDesc(Long memberNo);

    /**
     * 회원번호 + 연도로 1건 조회 (저장 시 덮어쓰기 판단용)
     */
    Optional<LeaveBalance> findByMember_MemberNoAndLeaveBalanceYear(Long memberNo, String leaveBalanceYear);
}


