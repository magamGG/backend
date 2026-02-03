package com.kh.magamGG.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectUpdateRequest {

    private String projectName;
    private String projectStatus;
    private String projectColor;
    private Integer projectCycle;
    private String thumbnailFile;
    private LocalDateTime projectStartedAt;
    private String projectGenre;
    private String platform;
    /** 작가 회원 번호 (MEMBER_NO) — 변경 시 PROJECT_MEMBER의 작가 역할 행 업데이트 */
    private Long artistMemberNo;
}
