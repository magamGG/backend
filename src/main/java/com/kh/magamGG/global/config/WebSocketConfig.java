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
        // 네이티브 WebSocket 엔드포인트 (SockJS 없이)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*"); // 테스트 단계에서는 모두 허용
        
        // SockJS 지원 엔드포인트 (호환성을 위해 유지)
        registry.addEndpoint("/ws-stomp-sockjs")
                .setAllowedOriginPatterns("*")
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