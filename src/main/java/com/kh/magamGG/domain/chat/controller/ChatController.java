package com.kh.magamGG.domain.chat.controller;

import com.kh.magamGG.domain.chat.dto.request.ChatMessageRequestDto;
import com.kh.magamGG.domain.chat.dto.response.ChatMessageResponseDto;
import com.kh.magamGG.domain.chat.dto.response.ChatRoomResponseDto;
import com.kh.magamGG.domain.chat.service.ChatMessageService;
import com.kh.magamGG.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@Slf4j
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
     * 2. 내 채팅방 목록 조회 (HTTP GET) - JWT에서 memberNo 추출
     */
    @GetMapping("/api/chat/rooms/me")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        log.info("💬 채팅방 목록 조회 요청 - 회원번호: {}", memberNo);
        
        try {
            List<ChatRoomResponseDto> rooms = chatRoomService.getMyChatRooms(memberNo);
            log.info("✅ 채팅방 목록 조회 성공 - 개수: {}", rooms.size());
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            log.error("❌ 채팅방 목록 조회 실패 - 회원번호: {}, 에러: {}", memberNo, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 3. 메시지 전송 (HTTP POST)
     */
    @PostMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<ChatMessageResponseDto> sendMessage(
            @PathVariable Long chatRoomNo,
            @RequestBody ChatMessageRequestDto request,
            Authentication authentication) {
        
        log.info("🔍 메시지 전송 API 호출됨 - 채팅방: {}", chatRoomNo);
        log.info("🔍 요청 본문: {}", request);
        log.info("🔍 인증 정보: {}", authentication);
        
        try {
            Long memberNo = (Long) authentication.getPrincipal();
            log.info("🔍 JWT에서 추출한 회원번호: {}", memberNo);
            
            log.info("💬 메시지 전송 요청 - 채팅방: {}, 회원: {}, 메시지: '{}'", 
                chatRoomNo, memberNo, request.getChatMessage());
            
            // DTO에 필요한 정보 설정
            request.setChatRoomNo(chatRoomNo);
            request.setMemberNo(memberNo);
            
            log.info("🔍 서비스 호출 전 - DTO: chatRoomNo={}, memberNo={}, message='{}'", 
                request.getChatRoomNo(), request.getMemberNo(), request.getChatMessage());
            
            ChatMessageResponseDto responseMessage = chatMessageService.saveMessage(request);
            log.info("✅ 메시지 전송 성공 - 메시지 번호: {}", responseMessage.getChatNo());
            
            // WebSocket으로도 브로드캐스트 (실시간 전송)
            messagingTemplate.convertAndSend("/topic/room/" + responseMessage.getChatRoomNo(), responseMessage);
            
            return ResponseEntity.ok(responseMessage);
        } catch (Exception e) {
            log.error("❌ 메시지 전송 실패 - 채팅방: {}, 에러 타입: {}, 메시지: {}", 
                chatRoomNo, e.getClass().getSimpleName(), e.getMessage());
            log.error("❌ 스택 트레이스:", e);
            throw e;
        }
    }

    /**
     * 4. 특정 방의 과거 대화 내역 조회 (HTTP GET, 페이징)
     * 예: /api/chat/rooms/1/messages?page=0&size=20
     */
    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<Slice<ChatMessageResponseDto>> getChatHistory(
            @PathVariable Long chatRoomNo,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("📋 채팅 내역 조회 요청 - 채팅방: {}, 페이지: {}, 크기: {}", 
            chatRoomNo, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Slice<ChatMessageResponseDto> history = chatMessageService.getChatHistory(chatRoomNo, pageable);
            log.info("✅ 채팅 내역 조회 성공 - 메시지 개수: {}, hasNext: {}", 
                history.getContent().size(), history.hasNext());
            
            // 메시지 내용도 로그로 출력
            history.getContent().forEach(msg -> 
                log.info("📨 메시지: ID={}, 발신자={}, 내용='{}'", 
                    msg.getChatNo(), msg.getSenderName(), msg.getChatMessage())
            );
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("❌ 채팅 내역 조회 실패 - 채팅방: {}, 에러: {}", chatRoomNo, e.getMessage(), e);
            throw e;
        }
    }
}