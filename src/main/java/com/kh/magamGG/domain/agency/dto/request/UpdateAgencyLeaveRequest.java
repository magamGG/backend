package com.kh.magamGG.domain.agency.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgencyLeaveRequest {
    private Integer agencyLeave; // 기본 연차 일수
}
