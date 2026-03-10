package com.kh.magamGG.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 아티스트 참여 프로젝트 정보 (포트폴리오 만들기 폼용)
 * 표시 형식: • 프로젝트명 (yyyy-MM-dd) - 역할 | 상태 | 플랫폼
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProjectItemDto {

    private Long projectNo;
    private String projectName;
    /** 프로젝트 시작일 (PROJECT_STARTED_AT). 표시용 날짜만 사용 */
    private LocalDateTime projectStartedAt;
    private LocalDateTime projectMemberCreatedAt;
    /** 프로젝트 상태 (연재/휴재/완결) */
    private String projectStatus;
    /** 연재 플랫폼 (카카오페이지, 네이버 웹툰 등) */
    private String platform;
    /** 해당 프로젝트에서의 역할 (작가, 담당자 등) */
    private String projectMemberRole;
}
