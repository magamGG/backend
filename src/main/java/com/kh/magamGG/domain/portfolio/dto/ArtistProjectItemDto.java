package com.kh.magamGG.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 아티스트 참여 프로젝트 정보 (포트폴리오 만들기 폼용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProjectItemDto {

    private Long projectNo;
    private String projectName;
    private LocalDateTime projectStartedAt;
    private LocalDateTime projectMemberCreatedAt;
}
