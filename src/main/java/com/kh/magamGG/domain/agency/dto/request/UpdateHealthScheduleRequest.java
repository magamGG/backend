package com.kh.magamGG.domain.agency.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 건강 검진 설정 수정 (HEALTH_SURVEY_PERIOD, HEALTH_SURVEY_CYCLE)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHealthScheduleRequest {

    /** 설문 기간(일) */
    private Integer period;
    /** 설문 주기(일) */
    private Integer cycle;
}
