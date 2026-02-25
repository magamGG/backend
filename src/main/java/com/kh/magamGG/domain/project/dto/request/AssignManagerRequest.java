package com.kh.magamGG.domain.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 담당자 배치 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignManagerRequest {
    private Long managerNo;
}
