package com.kh.magamGG.domain.chat.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDto {
    private Long chatRoomNo;
    private Long memberNo;
    private String message;        // 프론트엔드에서 보내는 필드명
    private String chatMessage;    // 기존 필드명 (하위 호환성)
    private String chatMessageType; // TALK, IMAGE 등
    
    // message 필드를 chatMessage로 매핑
    public String getChatMessage() {
        return this.chatMessage != null ? this.chatMessage : this.message;
    }
}