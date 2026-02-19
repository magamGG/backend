package com.kh.magamGG.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        log.info("🔍 JWT Filter - 요청: {} {}", method, requestURI);

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);
        
        if (token != null) {
            log.info("🔑 JWT 토큰 발견: {}...", token.substring(0, Math.min(token.length(), 20)));
            log.debug("🔑 전체 JWT 토큰: {}", token);
        } else {
            log.info("❌ JWT 토큰 없음");
        }

        // 2. 토큰이 있는 경우 처리
        if (StringUtils.hasText(token)) {
            try {
                // 2-1. Access Token 유효성 검증 (type 검증 포함)
                boolean isValid = jwtTokenProvider.validateAccessToken(token);
                log.info("토큰 검증 결과: {}", isValid);
                
                if (isValid) {
                    // 2-2. 토큰에서 회원번호 추출
                    Long memberNo = jwtTokenProvider.getMemberIdFromAccessToken(token);
                    log.info("추출된 회원번호: {}", memberNo);

                    // 2-3. SecurityContext에 인증 정보 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    memberNo,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("✅ JWT 인증 성공: memberNo={}, uri={}", memberNo, request.getRequestURI());
                } else {
                    // 토큰이 만료되었거나 유효하지 않음
                    log.warn("❌ JWT 토큰 검증 실패: uri={}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                    
                    // 인증이 필요한 요청인 경우에만 401 반환
                    if (requiresAuthentication(request)) {
                        log.warn("인증 실패로 401 반환: {}", request.getRequestURI());
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"message\":\"토큰이 만료되었거나 유효하지 않습니다.\"}");
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("❌ JWT 토큰 파싱 실패: {}", e.getMessage());
                // 토큰 파싱 실패 시 인증 실패 처리
                log.error("❌ JWT 토큰 파싱 실패: uri={}, error={}", request.getRequestURI(), e.getMessage(), e);
                SecurityContextHolder.clearContext();
                
                if (requiresAuthentication(request)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"유효하지 않은 토큰입니다.\"}");
                    return;
                }
            }
        } else {
            log.warn("⚠️ JWT 토큰 없음: uri={}, queryString={}", request.getRequestURI(), request.getQueryString());
        }

        // 3. 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더 또는 쿼리 파라미터에서 토큰 추출
     * 
     * 우선순위:
     * 1. Authorization 헤더 (Bearer 토큰)
     * 2. 쿼리 파라미터 token (SSE EventSource용)
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 먼저 확인
        String bearerToken = request.getHeader("Authorization");
        log.info("Authorization 헤더 체크: {}", bearerToken != null ? "있음" : "없음");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (StringUtils.hasText(token)) {
                log.info("✅ 헤더에서 토큰 추출 성공");
                return token;
            }
        }
        
        // 2. 쿼리 파라미터에서 token 확인 (SSE EventSource는 헤더를 보낼 수 없음)
        String queryToken = request.getParameter("token");
        log.info("쿼리 파라미터 'token' 체크: {}", queryToken != null ? "있음" : "없음");
        
        if (StringUtils.hasText(queryToken)) {
            log.info("✅ 쿼리 파라미터에서 토큰 추출 성공");
            return queryToken;
        }
        
        log.info("❌ 토큰 추출 실패: 헤더와 쿼리 파라미터 모두 없음");
        return null;
    }

    /**
     * 해당 요청이 인증이 필요한지 확인
     * permitAll 엔드포인트는 인증 실패해도 통과시킴
     */
    private boolean requiresAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // permitAll 엔드포인트 목록
        return !uri.startsWith("/api/auth/login") 
            && !uri.startsWith("/api/auth/refresh")
            && !uri.startsWith("/api/members")
            && !uri.startsWith("/uploads/");
    }
}
