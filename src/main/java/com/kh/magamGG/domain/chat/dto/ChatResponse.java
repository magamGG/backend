package com.kh.magamGG.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private ChatAction action;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatAction {
        private String actionType;  // section, attendance
        private String actionLabel;
        private String sectionKeyword;
    }
}
