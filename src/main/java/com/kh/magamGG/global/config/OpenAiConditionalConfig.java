package com.kh.magamGG.global.config;

import org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * OpenAI API 키가 설정된 경우에만 Spring AI OpenAI·ChatClient 자동 구성을 로드합니다.
 * 키가 없으면 이 설정이 로드되지 않아 앱이 정상 기동하고,
 * 포트폴리오 추출 호출 시 서비스에서 "API 키를 설정해주세요" 안내를 반환합니다.
 *
 * 설정: application-local.yaml에 spring.ai.openai.api-key: your-key 추가
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
@Import({ OpenAiAutoConfiguration.class, ChatClientAutoConfiguration.class })
public class OpenAiConditionalConfig {
}
