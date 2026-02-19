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
import java.util.Map;
import java.util.HashMap;

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
     * 2. 에이전시별 채팅방 목록 조회 (HTTP GET)
     * type이 "all"이면 같은 agencyNo의 모든 멤버가 포함된 채팅방들을 반환
     * 단, PROJECT 타입 채팅방은 해당 프로젝트에 참여한 멤버만 볼 수 있음
     */
    @GetMapping("/api/chat/rooms/agency/{agencyNo}")
    public ResponseEntity<List<ChatRoomResponseDto>> getChatRoomsByAgency(
            @PathVariable Long agencyNo,
            @RequestParam(defaultValue = "all") String type,
            @RequestHeader("X-Member-No") Long memberNo) {
        List<ChatRoomResponseDto> rooms = chatRoomService.getChatRoomsByAgency(agencyNo, type, memberNo);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 3. 내 채팅방 목록 조회 (HTTP GET) - 기존 메서드 유지
     */
    @GetMapping("/api/chat/rooms/{memberNo}")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(@PathVariable Long memberNo) {
        List<ChatRoomResponseDto> rooms = chatRoomService.getMyChatRooms(memberNo);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 4. 채팅방 입장 (HTTP POST)
     * 사용자를 채팅방 멤버로 등록
     */
    @PostMapping("/api/chat/rooms/{chatRoomNo}/join")
    public ResponseEntity<Void> joinChatRoom(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        chatRoomService.joinChatRoom(chatRoomNo, memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. 특정 방의 과거 대화 내역 조회 (HTTP GET, 페이징)
     * 예: /api/chat/rooms/1/messages?page=0&size=20
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<Slice<ChatMessageResponseDto>> getChatHistory(
            @PathVariable Long chatRoomNo,
            @PageableDefault(size = 20) Pageable pageable) {

        Slice<ChatMessageResponseDto> history = chatMessageService.getChatHistory(chatRoomNo, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * 6. 마지막으로 읽은 메시지 업데이트 (HTTP PUT)
     */
    @PutMapping("/api/chat/rooms/{chatRoomNo}/read")
    public ResponseEntity<Void> updateLastReadMessage(
            @PathVariable Long chatRoomNo,
            @RequestParam Long lastChatNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        chatRoomService.updateLastReadMessage(chatRoomNo, memberNo, lastChatNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 7. 특정 채팅방의 읽지 않은 메시지 개수 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long chatRoomNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        long unreadCount = chatRoomService.getUnreadCount(chatRoomNo, memberNo);
        return ResponseEntity.ok(unreadCount);
    }

    /**
     * 8. 특정 채팅방의 참여자 목록 조회 (HTTP GET)
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/members")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomMembers(
            @PathVariable Long chatRoomNo) {
        List<Map<String, Object>> members = chatRoomService.getChatRoomMembers(chatRoomNo);
        return ResponseEntity.ok(members);
    }

    /**
     * 9. 채팅 버튼 클릭 시 자동으로 채팅방 생성 및 참여자 초대 (HTTP POST)
     */
    @PostMapping("/api/chat/ensure-rooms")
    public ResponseEntity<Void> ensureChatRooms(
            @RequestHeader("X-Member-No") Long memberNo) {
        chatRoomService.ensureChatRoomsAndInviteMembers(memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 10. 간단한 채팅방 멤버 조회 (HTTP GET) - 프로필 정보 로그용
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/members/simple")
    public ResponseEntity<Void> getSimpleChatRoomMembers(
            @PathVariable Long chatRoomNo) {
        chatRoomService.logChatRoomMembers(chatRoomNo);
        return ResponseEntity.ok().build();
    }
}