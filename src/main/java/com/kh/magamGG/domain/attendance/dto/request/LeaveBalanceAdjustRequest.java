package com.kh.magamGG.domain.attendance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연차 조정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceAdjustRequest {

    /** 대상 회원 번호 */
    private Long memberNo;
    /** 조정 일수 (양수: 증가, 음수: 감소) */
    private Integer adjustment;
    /** 사유 (포상, 징계, 경력인정 등) */
    private String reason;
    /** 상세 메모 */
    private String note;
}
