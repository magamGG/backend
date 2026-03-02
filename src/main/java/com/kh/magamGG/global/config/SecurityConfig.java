package com.kh.magamGG.global.config;

import com.kh.magamGG.global.security.JwtAuthenticationFilter;
import com.kh.magamGG.global.security.OAuth2SuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security 설정
 * 
 * 주요 변경사항:
 * 1. OAuth2 로그인 설정 (조건부 활성화)
 * 2. OAuth2SuccessHandler 등록
 * 3. 토큰 재발급 엔드포인트 permitAll() 설정
 * 4. ClientRegistrationRepository가 없어도 서버 시작 가능
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    
    // ClientRegistrationRepository는 선택적 의존성 (없으면 OAuth2 비활성화)
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    /**
     * WebSocket(SockJS) - 인증 없이 허용. SockJS가 /ws-stomp, /ws-stomp/info 등으로 접근하므로 별도 체인으로 최우선 처리.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/ws-stomp", "/ws-stomp/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .build();
    }

    /**
     * 정적 리소스(uploads) - Notion 등 외부에서 이미지 로드 시 인증 없이 허용.
     * JWT 필터를 타지 않으며, 별도 체인으로 먼저 매칭된다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain staticResourceSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/uploads/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정 (SSE를 포함한 모든 요청에 적용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // CSRF 비활성화 (JWT는 Stateless이므로 CSRF 공격에 취약하지 않음)
            .csrf(csrf -> csrf.disable())

            // 세션 정책: STATELESS (JWT 사용)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // OAuth2 로그인 설정 (ClientRegistrationRepository가 있을 때만 활성화)
        if (clientRegistrationRepository != null) {
            log.info("✅ [SecurityConfig] OAuth2 로그인 활성화 (ClientRegistrationRepository 감지)");
            http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            );
        } else {
            log.warn("⚠️ [SecurityConfig] OAuth2 로그인 비활성화 (ClientRegistrationRepository 없음)");
            log.warn("⚠️ [SecurityConfig] 소셜 로그인은 커스텀 컨트롤러(/api/auth/{provider}/callback)를 통해 처리됩니다.");
        }

        http
            // 로그아웃 설정 비활성화 (컨트롤러에서 직접 처리)
            .logout(logout -> logout.disable())

            // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 엔드포인트별 인증 요구사항 설정 (인증 없이 호출하는 API)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/logout", "/api/auth/reissue").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(
                    "/api/members",
                    "/api/holidays/**",
                    "/login/oauth2/**",
                    "/oauth2/**"
                ).permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * 커스텀 OAuth2UserService
     * 기본적으로 Spring Security의 DefaultOAuth2UserService 사용
     */
    @Bean
    public org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService customOAuth2UserService() {
        return new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();
    }
}
