package com.kh.magamGG.global.config;

import com.kh.magamGG.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class  SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource, 
                         JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정 (SSE를 포함한 모든 요청에 적용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // CSRF 비활성화 (JWT는 Stateless이므로 CSRF 공격에 취약하지 않음)
            // 단, 쿠키 기반 인증을 사용한다면 활성화 필요
            .csrf(csrf -> csrf.disable())
            
            // 세션 정책: STATELESS (JWT 사용)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
            // 이렇게 하면 모든 요청이 JWT 필터를 거치게 됨
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 엔드포인트별 인증 요구사항 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/members").permitAll()
                .requestMatchers("/uploads/**").permitAll() // 정적 리소스 허용
                
                // 나머지는 인증 필요
                // /api/notifications/subscribe도 여기에 포함됨
                // JwtAuthenticationFilter에서 쿼리 파라미터 token을 읽어 인증 처리
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
