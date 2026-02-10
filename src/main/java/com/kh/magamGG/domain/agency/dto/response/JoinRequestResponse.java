package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {
    private Long newRequestNo;
    private Long agencyNo;
    private String agencyName;
    private Long memberNo;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
    private String memberRole;
    private LocalDateTime newRequestDate;
    private String newRequestStatus;
}
