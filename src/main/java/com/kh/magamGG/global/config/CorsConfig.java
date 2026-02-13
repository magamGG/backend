package com.kh.magamGG.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경: 프론트엔드 개발 서버 (Vite 포트 5173/5174 등)
        // 프로덕션: 실제 프론트엔드 도메인으로 변경 필요
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",  // Vite 다른 포트
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"
        ));

        // 허용할 HTTP 메서드 (SSE는 GET이므로 포함)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 허용할 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));
        
        // 자격 증명 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // 클라이언트가 읽을 수 있는 응답 헤더
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Last-Event-ID"));
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
