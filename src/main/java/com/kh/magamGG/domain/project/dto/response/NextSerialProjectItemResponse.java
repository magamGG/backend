package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아티스트 대시보드 "다음 연재 프로젝트"용 DTO.
 * PROJECT_MEMBER 소속 + PROJECT_STARTED_AT, PROJECT_CYCLE로 계산한 다음 연재일.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextSerialProjectItemResponse {
    private Long projectNo;
    private String projectName;
    private String projectColor;
    /** 다음 연재일 (yyyy-MM-dd, PROJECT_STARTED_AT + PROJECT_CYCLE 기준) */
    private String nextDeadline;
    /** 다음 연재일이 오늘인지 */
    private boolean today;
}
