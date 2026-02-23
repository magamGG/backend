package com.kh.magamGG.domain.chat.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponseDto {

    private Long chatRoomNo;       // 채팅방 번호
    private String chatRoomName;   // 채팅방 이름 (프로젝트명 등)
    private String chatRoomType;   // TOTAL, PROJECT 등

    // 마지막 메시지 정보 (목록 미리보기용)
    private String lastMessage;
    private String lastMessageTime; // 포맷팅된 시간 (예: "오후 2:30" 또는 "2월 12일")
    private Long lastMessageSenderNo; // 마지막 메시지 보낸 사람의 memberNo
    private String lastMessageSenderName; // 마지막 메시지 보낸 사람 이름

    // 안 읽은 메시지 개수
    private long unreadCount;

    // (선택 사항) 프로젝트 관련 정보
    private Long projectNo;
}