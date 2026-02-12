package com.kh.magamGG.domain.chat.controller;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.service.ChatMessageService;
import com.kh.magamGG.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 1. 실시간 메시지 전송 (WebSocket)
     * 클라이언트가 /app/chat/message로 보내면 이 메서드가 실행됨
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageRequestDto message) {
        // DB에 메시지 저장 (실명 포함된 ResponseDto 반환)
        ChatMessageResponseDto responseMessage = chatMessageService.saveMessage(message);

        // 해당 방을 구독 중인(/topic/room/{roomNo}) 유저들에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + responseMessage.getChatRoomNo(), responseMessage);
    }

    /**
     * 2. 내 채팅방 목록 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{memberNo}")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(@PathVariable Long memberNo) {
        List<ChatRoomResponseDto> rooms = chatRoomService.getMyChatRooms(memberNo);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 3. 특정 방의 과거 대화 내역 조회 (HTTP GET, 페이징)
     * 예: /api/chat/rooms/1/messages?page=0&size=20
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<Slice<ChatMessageResponseDto>> getChatHistory(
            @PathVariable Long chatRoomNo,
            @PageableDefault(size = 20) Pageable pageable) {

        Slice<ChatMessageResponseDto> history = chatMessageService.getChatHistory(chatRoomNo, pageable);
        return ResponseEntity.ok(history);
    }
}