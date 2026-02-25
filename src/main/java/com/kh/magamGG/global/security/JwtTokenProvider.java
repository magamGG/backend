package com.kh.magamGG.global.security;

import com.kh.magamGG.global.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.access-secret:${jwt.secret:}}")
    private String accessSecret;

    @Value("${jwt.refresh-secret:${jwt.secret:}}")
    private String refreshSecret;

    @Value("${jwt.access-expiration:${jwt.expiration:900000}}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    // 기존 호환성을 위한 필드
    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:900000}")
    private Long expiration;

    /**
     * 애플리케이션 시작 시 Secret Key 검증
     */
    @PostConstruct
    public void validateSecrets() {
        // access-secret이 없으면 secret 사용
        if ((accessSecret == null || accessSecret.trim().isEmpty()) && 
            (secret != null && !secret.trim().isEmpty())) {
            accessSecret = secret;
        }
        
        // refresh-secret이 없으면 secret 사용
        if ((refreshSecret == null || refreshSecret.trim().isEmpty()) && 
            (secret != null && !secret.trim().isEmpty())) {
            refreshSecret = secret;
        }
    }

    /**
     * Access Token용 서명 키 생성
     */
    private SecretKey getAccessSigningKey() {
        String key = (accessSecret != null && !accessSecret.trim().isEmpty()) ? accessSecret : secret;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Refresh Token용 서명 키 생성
     */
    private SecretKey getRefreshSigningKey() {
        String key = (refreshSecret != null && !refreshSecret.trim().isEmpty()) ? refreshSecret : secret;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 기존 호환성을 위한 메서드
     */
    private SecretKey getSigningKey() {
        return getAccessSigningKey();
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(Long memberId, String memberEmail) {
        Date now = new Date();
        Long exp = (accessExpiration != null && accessExpiration > 0) ? accessExpiration : expiration;
        Date expiryDate = new Date(now.getTime() + exp);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("email", memberEmail)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getAccessSigningKey())
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Long memberId) {
        Date now = new Date();
        Long exp = (refreshExpiration != null && refreshExpiration > 0) ? refreshExpiration : 604800000L;
        Date expiryDate = new Date(now.getTime() + exp);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getRefreshSigningKey())
                .compact();
    }

    /**
     * Refresh Token에서 회원번호 추출
     */
    public Long getMemberIdFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getRefreshSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (tokenType != null && !"refresh".equals(tokenType)) {
                throw new InvalidTokenException("Refresh Token이 아닙니다.");
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean validateRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            Jwts.parser()
                    .verifyWith(getRefreshSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰을 SHA-256 해시로 변환
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("토큰 해시 생성 실패", e);
        }
    }

    /**
     * Refresh Token 만료 시간 반환 (밀리초)
     */
    public Long getRefreshExpiration() {
        return (refreshExpiration != null && refreshExpiration > 0) ? refreshExpiration : 604800000L;
    }

    // 기존 메서드 (하위 호환성 유지)
    public String generateToken(Long memberNo, String memberEmail) {
        return generateAccessToken(memberNo, memberEmail);
    }

    public Long getMemberNoFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

