package com.kh.magamGG.domain.chat.dto.response;

import com.kh.magamGG.domain.chat.entity.ChatMessage;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
public class ChatMessageResponseDto {
    private Long chatNo;
    private Long chatRoomNo;
    private Long memberNo;
    private String senderName;    // 화면에 띄울 닉네임
    private String senderProfile; // 화면에 띄울 프로필 이미지 경로 (선택)
    private String chatMessage;
    private String chatMessageType;
    private String createdAt;     // 리액트에서 쓰기 좋게 포맷팅된 시간

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static ChatMessageResponseDto from(ChatMessage entity) {
        String profileImage = entity.getMember().getMemberProfileImage();
        System.out.println("🖼️ 프로필 이미지 정보 - 회원: " + entity.getMember().getMemberName() + ", 이미지: " + profileImage);
        
        return ChatMessageResponseDto.builder()
                .chatNo(entity.getChatNo())
                .chatRoomNo(entity.getChatRoom().getChatRoomNo())
                .memberNo(entity.getMember().getMemberNo())
                .senderName(entity.getMember().getMemberName())
                .senderProfile(profileImage) // 프로필 이미지 추가
                .chatMessage(entity.getChatMessage())
                .chatMessageType(entity.getChatMessageType())
                .createdAt(entity.getChatMessageCreatedAt().toString()) // ISO 형식으로 변경
                .build();
    }
}