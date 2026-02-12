package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.OllamaChatRequest;
import com.kh.magamGG.domain.chat.dto.OllamaChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final WebClient ollamaWebClient;

    @Value("${ollama.model}")
    private String model;

    /**
     * Ollama Chat API(/api/chat)를 호출하여 AI 응답을 생성합니다.
     * Generate API 대비 더 안정적입니다.
     */
    public String generate(String prompt, String systemPrompt) {
        try {
            List<OllamaChatRequest.ChatMessage> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(OllamaChatRequest.ChatMessage.builder()
                        .role("system")
                        .content(systemPrompt)
                        .build());
            }
            messages.add(OllamaChatRequest.ChatMessage.builder()
                    .role("user")
                    .content(prompt)
                    .build());

            OllamaChatRequest request = OllamaChatRequest.builder()
                    .model(model)
                    .messages(messages)
                    .stream(false)
                    .options(OllamaChatRequest.Options.builder().temperature(0.2).build())
                    .build();

            OllamaChatResponse response = ollamaWebClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block();

            if (response != null && response.getMessage() != null && response.getMessage().getContent() != null) {
                return response.getMessage().getContent().trim();
            }
            return "죄송해요, 응답을 생성하지 못했어요.";
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Ollama API 호출 실패: status={}, body={}", e.getStatusCode(), body);
            int status = e.getStatusCode().value();
            if (status == 404 || (body != null && (body.contains("not found") || body.contains("model not found")))) {
                return "AI 모델(" + model + ")이 설치되지 않았어요.\n\nPowerShell에서 실행해 주세요:\nollama pull " + model;
            }
            if (status == 500 && body != null && body.contains("memory")) {
                return "메모리가 부족해요. application.yaml에서 model을 qwen2.5:1.5b로 변경한 뒤 'ollama pull qwen2.5:1.5b'를 실행해 주세요.";
            }
            if (status == 500) {
                return "AI 응답 생성 중 오류가 발생했어요. Ollama를 재시작해 보세요.";
            }
            return "AI 서비스에 연결할 수 없어요. 잠시 후 다시 시도해주세요.";
        } catch (Exception e) {
            log.error("Ollama API 호출 실패: {}", e.getMessage(), e);
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || e.getMessage().contains("connect"))) {
                return "Ollama가 실행 중이지 않은 것 같아요.\nOllama를 시작한 뒤 다시 시도해 주세요.";
            }
            return "AI 서비스에 연결할 수 없어요. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * Ollama 서버 연결 상태를 확인합니다.
     */
    public boolean isAvailable() {
        try {
            ollamaWebClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Ollama 서버 연결 실패: {}", e.getMessage());
            return false;
        }
    }
}
