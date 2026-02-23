package com.kh.magamGG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;

/**
 * OpenAI·ChatClient 자동 구성은 API 키가 없을 때도 앱이 기동되도록 제외.
 * API 키 설정 시에만 {@link com.kh.magamGG.global.config.OpenAiConditionalConfig}에서 로드됨.
 */
@SpringBootApplication(exclude = {
		OpenAiAutoConfiguration.class,
		ChatClientAutoConfiguration.class
})
public class MagamGgApplication {

	public static void main(String[] args) {
		SpringApplication.run(MagamGgApplication.class, args);
	}

}
