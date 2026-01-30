package com.kh.magamGG.domain.health.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일일 건강 체크 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyHealthCheckRequest {
    
    private String healthCondition;  // 컨디션 (피곤함, 보통, 좋음, 최상)
    private Integer sleepHours;       // 수면 시간
    private Integer discomfortLevel;  // 신체 불편함 정도 (0-10)
    private String healthCheckNotes;  // 메모
}
