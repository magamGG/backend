package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트에 담당자로 배치 가능한 담당자 정보 (ARTIST_ASSIGNMENT 기준)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignableManagerResponse {
    private Long managerNo;
    private Long memberNo;
    private String memberName;
}
