package com.kh.magamGG.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private boolean stream;
    /** temperature 0.2: 환각 감소, 숫자·이름 등 사실 정보 정확도 향상 */
    private Options options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {
        private Double temperature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;  // "system", "user", "assistant"
        private String content;
    }
}
