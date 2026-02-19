package com.kh.magamGG.domain.chat.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private Long chatRoomNo;
    private Long memberNo;
    private String chatMessage;
    private String chatMessageType; // TALK, IMAGE 등
}