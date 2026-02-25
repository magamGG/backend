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

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 애플리케이션 시작 시 Secret Key 검증
     * 환경변수가 없거나 빈 값이면 IllegalStateException 발생
     */
    @PostConstruct
    public void validateSecrets() {
        if (accessSecret == null || accessSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT Access Secret이 설정되지 않았습니다. " +
                "환경변수 JWT_ACCESS_SECRET을 설정해주세요."
            );
        }

        if (refreshSecret == null || refreshSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT Refresh Secret이 설정되지 않았습니다. " +
                "환경변수 JWT_REFRESH_SECRET을 설정해주세요."
            );
        }

        // Secret Key 최소 길이 검증 (Base64 인코딩 기준 최소 32자)
        if (accessSecret.length() < 32) {
            throw new IllegalStateException(
                "JWT Access Secret이 너무 짧습니다. " +
                "최소 256bit (Base64 인코딩 시 32자 이상)이 필요합니다."
            );
        }

        if (refreshSecret.length() < 32) {
            throw new IllegalStateException(
                "JWT Refresh Secret이 너무 짧습니다. " +
                "최소 256bit (Base64 인코딩 시 32자 이상)이 필요합니다."
            );
        }

        // Access와 Refresh Secret이 동일한지 검증 (보안상 분리 필요)
        if (accessSecret.equals(refreshSecret)) {
            throw new IllegalStateException(
                "JWT Access Secret과 Refresh Secret은 서로 달라야 합니다."
            );
        }
    }

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
     * type 클레임이 "access"인지도 함께 검증
     * 모든 JWT 예외를 처리하여 false 반환
     */
    public boolean validateAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getAccessSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // type 클레임 검증: access 토큰인지 확인
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                return false; // access 타입이 아니면 false
            }
            
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 만료된 토큰
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // 잘못된 형식의 토큰
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            // 지원하지 않는 토큰
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 서명 검증 실패
            return false;
        } catch (IllegalArgumentException e) {
            // 빈 문자열 또는 null
            return false;
        } catch (Exception e) {
            // 기타 예외
            return false;
        }
    }

    /**
     * Refresh Token 유효성 검증
     * 모든 JWT 예외를 처리하여 false 반환
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
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 만료된 토큰
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // 잘못된 형식의 토큰
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            // 지원하지 않는 토큰
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 서명 검증 실패
            return false;
        } catch (IllegalArgumentException e) {
            // 빈 문자열 또는 null
            return false;
        } catch (Exception e) {
            // 기타 예외
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
     * DB 저장 시 만료 시간 동기화를 위해 사용
     */
    public Long getRefreshExpiration() {
        return refreshExpiration;
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
