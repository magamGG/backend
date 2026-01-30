package com.kh.magamGG.domain.agency.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RejectJoinRequestRequest {
    private String rejectionReason; // 거절 사유
}
