package com.kh.magamGG.domain.chat.controller;

import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.dto.QuickReportResponse;
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

    /**
     * 챗봇 퀵 리포트 (역할별 버튼 클릭 시 메시지 + 길 안내 버튼 반환)
     * GET /api/chat/quick-report?type=compliance_top3|deadline_urgent|leave_balance|...
     */
    @GetMapping("/quick-report")
    public ResponseEntity<QuickReportResponse> getQuickReport(
            @RequestParam String type,
            @RequestHeader(value = "X-Member-No", required = false) Long memberNo) {
        QuickReportResponse response = chatService.getQuickReport(type, memberNo);
        if (response == null || response.getMessage() == null) {
            response = QuickReportResponse.builder().message("조회 결과가 없습니다.").build();
        }
        return ResponseEntity.ok(response);
    }
}
