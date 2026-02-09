package com.kh.magamGG.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 담당자 대시보드용: 현재 날짜에 출근 중인(마지막 이력이 '출근') 배정 작가 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingArtistResponse {
    private Long memberNo;
    private String memberName;
    /** 오늘 출근한 시각 (ATTENDANCE 출근 기록 기준) */
    private LocalDateTime clockInTime;
}
