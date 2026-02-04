package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에이전시 건강 검진 일정 (HEALTH_SURVEY 생성일·주기 기반 다음 검진 예정일)
 * 정신/신체 동일 주기 사용 시 하나의 예정일로 공통 표시
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyHealthScheduleResponse {

    /** 다음 검진 예정일 (yyyy.MM.dd) */
    private String nextCheckupDate;
    /** 다음 검진까지 남은 일수 */
    private Integer daysUntil;
    /** 설문 기간(일) - HEALTH_SURVEY_PERIOD */
    private Integer period;
    /** 설문 주기(일) - HEALTH_SURVEY_CYCLE */
    private Integer cycle;
    /** 설정 생성일 (yyyy.MM.dd) - 참고용 */
    private String createdAt;
}
