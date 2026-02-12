package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.ChatRequest;
import com.kh.magamGG.domain.chat.dto.ChatResponse;

public interface ChatService {
    ChatResponse processChat(ChatRequest request, Long memberNo);
    boolean isAIAvailable();
}
