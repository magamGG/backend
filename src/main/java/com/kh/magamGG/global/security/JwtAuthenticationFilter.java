package com.kh.magamGG.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰이 있고 유효하면 인증 처리
        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            try {
                // 3. 토큰에서 회원번호 추출
                Long memberNo = jwtTokenProvider.getMemberIdFromAccessToken(token);

                // 4. SecurityContext에 인증 정보 저장
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                memberNo,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 토큰 파싱 실패 시 인증 실패 처리
                SecurityContextHolder.clearContext();
            }
        }

        // 5. 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더 또는 쿼리 파라미터에서 토큰 추출
     * EventSource(SSE)는 커스텀 헤더를 보낼 수 없으므로 /api/notifications/subscribe 는 token 쿼리 파라미터 사용
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // SSE 구독: EventSource는 헤더 불가 → token 쿼리 파라미터
        if (request.getRequestURI() != null && request.getRequestURI().contains("/api/notifications/subscribe")) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                return tokenParam;
            }
        }
        return null;
    }
}
