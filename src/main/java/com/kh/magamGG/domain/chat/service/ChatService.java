package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;
import com.kh.magamGG.domain.chat.dto.QuickReportResponse;

public interface ChatService {
    ChatResponse processChat(ChatRequest request, Long memberNo);
    boolean isAIAvailable();

    /**
     * 역할별 퀵 리포트 (메시지 + 길 안내 버튼)
     * @param type compliance_top3, deadline_urgent, leave_balance, ...
     */
    QuickReportResponse getQuickReport(String type, Long memberNo);
}
