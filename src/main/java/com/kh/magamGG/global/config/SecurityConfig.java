package com.kh.magamGG.global.config;

import com.kh.magamGG.global.security.JwtAuthenticationFilter;
import com.kh.magamGG.global.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security 설정
 * 
 * 주요 변경사항:
 * 1. OAuth2 로그인 설정 추가
 * 2. OAuth2SuccessHandler 등록
 * 3. 토큰 재발급 엔드포인트 permitAll() 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
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

            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)  // 구글 로그인 성공 핸들러
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())  // 기본 OAuth2UserService 사용
                )
            )

            // 로그아웃 설정 비활성화 (컨트롤러에서 직접 처리)
            // Spring Security의 기본 logout 필터는 리다이렉트를 수행하므로 비활성화
            .logout(logout -> logout.disable())

            // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
            // 이렇게 하면 모든 요청이 JWT 필터를 거치게 됨
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 엔드포인트별 인증 요구사항 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/logout",  // 로그아웃 엔드포인트 추가 (컨트롤러에서 처리)
                    "/api/auth/refresh",
                    "/api/auth/reissue",  // 토큰 재발급 엔드포인트 추가
                    "/api/members",
                    "/api/auth/email/**",
                    "/api/auth/forgot-password",
                    "/api/auth/verify-reset-code",
                    "/api/auth/reset-password",
                    "/api/holidays/**",
                    "/login/oauth2/**"  // OAuth2 로그인 엔드포인트
                ).permitAll()
                .requestMatchers("/uploads/**").permitAll() // 정적 리소스 허용
                .requestMatchers("/ws-stomp/**").permitAll() // WebSocket 엔드포인트 허용
                .anyRequest().authenticated() // 나머지는 인증 필요
            );

        return http.build();
    }

    /**
     * 커스텀 OAuth2UserService (필요시 구현)
     * 기본적으로 Spring Security의 DefaultOAuth2UserService 사용
     */
    @Bean
    public org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService customOAuth2UserService() {
        return new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();
    }
}
