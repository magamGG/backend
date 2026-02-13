package com.kh.magamGG.global.config;

import com.kh.magamGG.global.security.JsonAccessDeniedHandler;
import com.kh.magamGG.global.security.JsonAuthenticationEntryPoint;
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
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                          JsonAccessDeniedHandler jsonAccessDeniedHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jsonAuthenticationEntryPoint = jsonAuthenticationEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
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
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                .accessDeniedHandler(jsonAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/members", "/api/auth/email/**",
                                 "/api/auth/forgot-password", "/api/auth/verify-reset-code", "/api/auth/reset-password").permitAll() // 로그인, 토큰 갱신, 회원가입, 이메일 인증, 비밀번호 찾기는 인증 없이 접근 가능
                .requestMatchers("/uploads/**").permitAll() // 정적 리소스 허용
                .anyRequest().authenticated() // 나머지는 인증 필요
            );

        return http.build();
    }
}
