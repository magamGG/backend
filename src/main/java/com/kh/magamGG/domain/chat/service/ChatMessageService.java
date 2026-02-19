package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {


    ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto);

    // 채팅 내역 조회 (Slice를 이용한 페이징)
    Slice<ChatMessageResponseDto> getChatHistory(Long chatRoomNo, Pageable pageable);

    // 안 읽은 메시지 수 확인
    long getUnreadCount(Long chatRoomNo, Long memberNo);
}
