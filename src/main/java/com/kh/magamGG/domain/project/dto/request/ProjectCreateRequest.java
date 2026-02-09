package com.kh.magamGG.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 생성 요청 DTO
 * PROJECT + PROJECT_MEMBER 동시 생성
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCreateRequest {

    /**
     * 프로젝트명 (필수)
     * DB: PROJECT_NAME
     */
    private String projectName;

    /**
     * 프로젝트 상태 (기본값: 연재)
     * DB: PROJECT_STATUS - 연재 / 휴재 / 완결
     */
    private String projectStatus;

    /**
     * 프로젝트 색상 (기본값: 기본색)
     * DB: PROJECT_COLOR
     */
    private String projectColor;

    /**
     * 연재 주기 (일 단위, 선택)
     * DB: PROJECT_CYCLE
     */
    private Integer projectCycle;

    /**
     * 썸네일 파일 URL (선택)
     * DB: THUMBNAIL_FILE
     */
    private String thumbnailFile;

    /**
     * 연재 시작일 (선택)
     * DB: PROJECT_STARTED_AT
     */
    private LocalDateTime projectStartedAt;

    /**
     * 작가 회원 번호 (필수) - 대표 작가, PROJECT_MEMBER에 '작가' 역할로 등록
     * DB: MEMBER_NO in PROJECT_MEMBER
     */
    private Long artistMemberNo;

    /**
     * 장르 (선택)
     * DB: PROJECT_GENRE
     */
    private String projectGenre;

    /**
     * 플랫폼 (선택)
     * DB: PLATFORM
     */
    private String platform;
}
