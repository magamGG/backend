package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectListResponse {

    private Long projectNo;
    private String projectName;
    private String projectStatus;
    private String projectColor;
    private String thumbnailFile;
    private String artistName;
    private Long artistMemberNo;
    private String projectGenre;
    private String platform;
    private Integer projectCycle;
    private LocalDateTime projectStartedAt;
}
