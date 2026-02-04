package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마감 임박 현황용 DTO (주기 기준 계산)
 * 오늘/내일/2일 후/3일 후/4일 후별 마감 예정 건수
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlineCountResponse {

    private String name;  // "오늘", "내일", "2일 후", "3일 후", "4일 후"
    private int count;
}
