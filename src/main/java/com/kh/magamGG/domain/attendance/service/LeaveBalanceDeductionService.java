package com.kh.magamGG.domain.attendance.service;

import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import com.kh.magamGG.domain.attendance.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 연차 잔액 차감 비즈니스 로직 분리
 * 가이드 준수: attendance 도메인 내 새 클래스로 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveBalanceDeductionService {
    
    private final LeaveBalanceRepository leaveBalanceRepository;
    
    /**
     * 반차/반반차 승인 시 연차 잔액 차감
     * @param memberNo 회원 번호
     * @param requestType 요청 타입 ("반차" 또는 "반반차")
     * @throws IllegalArgumentException 연차 잔액이 없거나 부족한 경우
     */
    @Transactional
    @CacheEvict(value = "leaveBalance", key = "#memberNo")
    public void deductLeaveBalance(Long memberNo, String requestType) {
        if (memberNo == null) {
            throw new IllegalArgumentException("회원 번호는 필수입니다.");
        }
        
        double deduction = "반차".equals(requestType) ? 0.5 : 0.25;
        
        LeaveBalance balance = leaveBalanceRepository
                .findTop1ByMember_MemberNoOrderByLeaveBalanceYearDesc(memberNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 회원의 연차 잔액이 없습니다. 연차를 먼저 부여해주세요."));
        
        double currentRemain = balance.getLeaveBalanceRemainDays() != null 
                ? balance.getLeaveBalanceRemainDays() 
                : 0.0;
        double newRemain = currentRemain - deduction;
        
        if (newRemain < 0) {
            throw new IllegalArgumentException(
                    String.format("연차 잔액이 부족합니다. (필요: %.2f일, 남은 양: %.2f일)", 
                            deduction, currentRemain));
        }
        
        balance.setLeaveBalanceRemainDays(newRemain);
        balance.setLeaveBalanceUpdatedAt(LocalDateTime.now());
        leaveBalanceRepository.save(balance);
        
        log.info("연차 잔액 차감 완료: 회원번호={}, 타입={}, 차감={}, 차감 후 잔여={}", 
                memberNo, requestType, deduction, newRemain);
    }
}
