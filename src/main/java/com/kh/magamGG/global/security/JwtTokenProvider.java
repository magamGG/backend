package com.kh.magamGG.global.security;

import com.kh.magamGG.global.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Access Token용 서명 키 생성
     */
    private SecretKey getAccessSigningKey() {
        byte[] keyBytes = accessSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Refresh Token용 서명 키 생성
     */
    private SecretKey getRefreshSigningKey() {
        byte[] keyBytes = refreshSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(Long memberId, String memberEmail) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

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
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getRefreshSigningKey())
                .compact();
    }

    /**
     * Access Token에서 회원번호 추출
     */
    public Long getMemberIdFromAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getAccessSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                throw new InvalidTokenException("Access Token이 아닙니다.");
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
        }
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
            if (!"refresh".equals(tokenType)) {
                throw new InvalidTokenException("Refresh Token이 아닙니다.");
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }
    }

    /**
     * Access Token 유효성 검증
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getAccessSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean validateRefreshToken(String token) {
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

    // 기존 메서드 (하위 호환성 유지)
    @Deprecated
    public String generateToken(Long memberNo, String memberEmail) {
        return generateAccessToken(memberNo, memberEmail);
    }

    @Deprecated
    public Long getMemberNoFromToken(String token) {
        return getMemberIdFromAccessToken(token);
    }

    @Deprecated
    public boolean validateToken(String token) {
        return validateAccessToken(token);
    }
}
