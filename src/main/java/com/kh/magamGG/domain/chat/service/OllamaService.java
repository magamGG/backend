package com.kh.magamGG.domain.chat.service;

import com.kh.magamGG.domain.chat.dto.OllamaRequest;
import com.kh.magamGG.domain.chat.dto.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final WebClient ollamaWebClient;

    @Value("${ollama.model}")
    private String model;

    /**
     * Ollama API를 호출하여 AI 응답을 생성합니다.
     */
    public String generate(String prompt, String systemPrompt) {
        try {
            OllamaRequest request = OllamaRequest.builder()
                    .model(model)
                    .prompt(prompt)
                    .system(systemPrompt)
                    .stream(false)
                    .build();

            OllamaResponse response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block();

            if (response != null && response.getResponse() != null) {
                return response.getResponse().trim();
            }
            return "죄송해요, 응답을 생성하지 못했어요.";
        } catch (Exception e) {
            log.error("Ollama API 호출 실패: {}", e.getMessage());
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
