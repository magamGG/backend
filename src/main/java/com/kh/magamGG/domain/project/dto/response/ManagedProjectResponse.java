package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 담당자 대시보드 담당 프로젝트 현황용 DTO
 * GUIDE: projectNo, projectName, artist(담당 작가), status(정상/주의), progress, deadline
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagedProjectResponse {

    private Long projectNo;
    private String projectName;
    private String artist;      // 담당 작가명
    private String status;      // "정상" | "주의"
    private Integer progress;   // 0-100 진행률
    private String deadline;    // "1월 25일" 형식
}
