package com.kh.magamGG.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 챗봇 퀵 리포트 응답 (메시지 + 길 안내 버튼)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickReportResponse {
    private String message;
    /** 상세 보기 등 네비게이션 버튼 (label, sectionKeyword) */
    private List<ActionItem> actions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String label;
        private String sectionKeyword;
    }
}
