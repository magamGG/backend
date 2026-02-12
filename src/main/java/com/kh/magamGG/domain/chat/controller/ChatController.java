package com.kh.magamGG.domain.chat.controller;

import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * AI 채팅 메시지 처리
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Member-No", required = false) Long memberNo) {

        ChatResponse response = chatService.processChat(request, memberNo);
        return ResponseEntity.ok(response);
    }

    /**
     * AI 서비스 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean available = chatService.isAIAvailable();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "AI 서비스가 정상 작동 중입니다." : "AI 서비스에 연결할 수 없습니다."
        ));
    }
}
