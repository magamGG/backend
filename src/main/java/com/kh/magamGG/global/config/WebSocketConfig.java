package com.kh.magamGG.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 이 어노테이션이 SimpMessageSendingOperations를 만들어줍니다!
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트엔드에서 SockJS로 연결할 엔드포인트 설정
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 테스트 단계에서는 모두 허용
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173") // 명시적으로 허용할 Origin 추가
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 때 (구독): /topic으로 시작하는 메시지를 브로커가 처리
        registry.enableSimpleBroker("/topic");

        // 메시지를 보낼 때 (발신): /app으로 시작하는 메시지는 @MessageMapping 메서드로 전달
        registry.setApplicationDestinationPrefixes("/app");
    }
}