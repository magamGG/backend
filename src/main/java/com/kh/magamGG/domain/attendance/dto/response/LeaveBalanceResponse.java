package com.kh.magamGG.domain.attendance.dto.response;

import com.kh.magamGG.domain.attendance.entity.LeaveBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연차 잔액 응답 DTO
 * DB: LEAVE_BALANCE
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {

    private Long leaveBalanceNo;
    private Long memberNo;
    private Integer leaveBalanceTotalDays;
    private Integer leaveBalanceUsedDays;
    private Double leaveBalanceRemainDays;
    private String leaveBalanceYear;

    public static LeaveBalanceResponse fromEntity(LeaveBalance entity) {
        if (entity == null) return null;
        Long memberNo = entity.getMember() != null ? entity.getMember().getMemberNo() : null;
        return LeaveBalanceResponse.builder()
                .leaveBalanceNo(entity.getLeaveBalanceNo())
                .memberNo(memberNo)
                .leaveBalanceTotalDays(entity.getLeaveBalanceTotalDays() != null ? entity.getLeaveBalanceTotalDays() : 0)
                .leaveBalanceUsedDays(entity.getLeaveBalanceUsedDays() != null ? entity.getLeaveBalanceUsedDays() : 0)
                .leaveBalanceRemainDays(entity.getLeaveBalanceRemainDays() != null ? entity.getLeaveBalanceRemainDays() : 0.0)
                .leaveBalanceYear(entity.getLeaveBalanceYear() != null ? entity.getLeaveBalanceYear() : "")
                .build();
    }
}
