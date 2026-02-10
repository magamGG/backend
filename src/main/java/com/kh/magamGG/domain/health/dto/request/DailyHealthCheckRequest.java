package com.kh.magamGG.domain.health.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "수면 시간은 필수입니다.")
    @Min(value = 0, message = "수면 시간은 0시간 이상이어야 합니다.")
    @Max(value = 24, message = "수면 시간은 24시간 이하여야 합니다.")
    private Integer sleepHours;       // 수면 시간

    private Integer discomfortLevel;  // 신체 불편함 정도 (0-10)
    private String healthCheckNotes;  // 메모
}
