package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 미검진 인원 목록 (정신/신체 중 하나라도 미검진이면 포함)
 * status: BOTH(전체 안함), MENTAL_ONLY(정신만 안함), PHYSICAL_ONLY(신체만 안함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyUnscreenedListResponse {

    /** 다음 검진 예정일 (yyyy.MM.dd) - 참고용 */
    private String nextCheckupDate;
    private List<UnscreenedItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnscreenedItem {
        private Long memberNo;
        private String memberName;
        private String position;
        /** BOTH | MENTAL_ONLY | PHYSICAL_ONLY */
        private String status;
        private String lastMentalCheckDate;
        private String lastPhysicalCheckDate;
        /** health_survey 생성일·cycle·period 기준 현재 구간 마감일 대비 지연 일수 (0 이상) */
        private Integer daysOverdue;
    }
}
