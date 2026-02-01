package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDetailResponse {
    private Long agencyNo;
    private String agencyName;
    private String agencyCode;
    private Integer agencyLeave;
}
