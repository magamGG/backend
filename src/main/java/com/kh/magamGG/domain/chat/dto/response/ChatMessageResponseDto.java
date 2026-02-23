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
    private String attachmentUrl; // 첨부파일 URL
    private String createdAt;     // 리액트에서 쓰기 좋게 포맷팅된 시간

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static ChatMessageResponseDto from(ChatMessage entity) {
        String profileImage = entity.getMember().getMemberProfileImage();
        
        return ChatMessageResponseDto.builder()
                .chatNo(entity.getChatNo())
                .chatRoomNo(entity.getChatRoom().getChatRoomNo())
                .memberNo(entity.getMember().getMemberNo())
                .senderName(entity.getMember().getMemberName())
                .senderProfile(profileImage)
                .chatMessage(entity.getChatMessage())
                .chatMessageType(entity.getChatMessageType())
                .attachmentUrl(entity.getAttachmentUrl())
                .createdAt(entity.getChatMessageCreatedAt().format(DateTimeFormatter.ofPattern("a h:mm"))) // "오후 2:30" 형식
                .build();
    }
}