package com.kh.magamGG.global.config;

import com.kh.magamGG.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
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
        log.info("🔧 Security 설정 초기화");
        
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
            
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("🚫 인증 실패 - URI: {}, 에러: {}", request.getRequestURI(), authException.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"인증이 필요합니다\",\"message\":\"로그인이 필요합니다\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("🚫 접근 거부 - URI: {}, 에러: {}", request.getRequestURI(), accessDeniedException.getMessage());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"접근 권한이 없습니다\",\"message\":\"권한이 부족합니다\"}");
                })
            )

            // 엔드포인트별 인증 요구사항 설정
            .authorizeHttpRequests(auth -> {
                log.info("🛡️ Security 규칙 설정:");
                log.info("  - /api/auth/login, /api/auth/refresh, /api/members: 인증 불필요");
                log.info("  - /uploads/**: 인증 불필요");
                log.info("  - /api/**: 인증 필요");
                log.info("  - 나머지: 허용 (프론트엔드)");
                
                auth
                    .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/members", "/api/auth/email/**",
                                     "/api/auth/forgot-password", "/api/auth/verify-reset-code", "/api/auth/reset-password",
                                     "/api/holidays/**").permitAll() // 로그인, 토큰 갱신, 회원가입, 이메일 인증, 비밀번호 찾기, 공휴일 API는 인증 없이 접근 가능
                    .requestMatchers("/uploads/**").permitAll() // 정적 리소스 허용
                    .requestMatchers("/api/**").authenticated() // API는 인증 필요
                    .anyRequest().permitAll(); // 프론트엔드 라우팅을 위해 나머지는 허용
            });

        return http.build();
    }
}
