package com.kh.magamGG.domain.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에이전시 소속 회원별 연차 잔액 응답 DTO
 * 연차 관리 페이지 직원 리스트용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMemberLeaveResponse {

    private Long memberNo;
    private String memberName;
    private String memberRole;
    private Integer leaveBalanceTotalDays;
    private Integer leaveBalanceUsedDays;
    private Double leaveBalanceRemainDays;
    private String leaveBalanceYear;
    private Long leaveBalanceNo;
    /** 당해 연도 leave_history 조정 이력 합계 (포상/징계/경력인정 등) */
    private Integer currentYearAdjustmentTotal;
}
