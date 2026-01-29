package com.kh.magamGG.domain.attendance.repository;

import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByMember_MemberNoOrderByLeaveBalanceYearDesc(Long memberNo);
}


