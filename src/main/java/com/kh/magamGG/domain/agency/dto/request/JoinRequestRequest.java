package com.kh.magamGG.domain.agency.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JoinRequestRequest {
    private String agencyCode;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
}
