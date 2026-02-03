package com.kh.magamGG.domain.agency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에이전시 대시보드 - 작품별 아티스트 분포 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistDistributionResponse {

    /** 작품(프로젝트)별 아티스트 수 [{ name: "로맨스 판타지", artists: 5 }, ...] */
    private List<ArtistDistributionItem> distribution;

    /** 가장 많은 아티스트가 배정된 작품명 (하단 안내 문구용) */
    private String maxArtistProjectName;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArtistDistributionItem {
        private String name;
        private Long artists;
    }
}
