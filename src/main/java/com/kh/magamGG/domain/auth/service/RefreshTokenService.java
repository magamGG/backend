package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.global.exception.InvalidTokenException;
import com.kh.magamGG.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스 (Valkey 기반)
 * 
 * 주요 기능:
 * - Refresh Token을 Valkey에 저장 (키: RT:{email})
 * - Refresh Token Rotation 시 기존 토큰 삭제 후 새 토큰 저장
 * - 토큰 재사용 공격 감지 (Valkey에 토큰이 없으면 탈취된 것으로 간주)
 * 
 * 보안 고려사항:
 * 1. 토큰은 해시값으로 저장 (평문 저장 방지)
 * 2. TTL은 jwt.refresh-expiration과 동기화
 * 3. 토큰 Rotation 시 원자적 연산 (기존 토큰 삭제 → 새 토큰 저장)
 * 4. 토큰 불일치 시 모든 세션 무효화 (보안 강화)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    /**
     * Refresh Token을 Valkey에 저장
     * 
     * @param email 사용자 이메일
     * @param refreshToken Refresh Token (평문)
     * 
     * 저장 형식:
     * - Key: RT:{email}
     * - Value: Refresh Token의 SHA-256 해시값
     * - TTL: jwt.refresh-expiration (밀리초)
     */
    public void saveRefreshToken(String email, String refreshToken) {
        if (email == null || email.isEmpty()) {
            log.error("❌ [RefreshTokenService] 이메일이 null이거나 비어있음");
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.error("❌ [RefreshTokenService] Refresh Token이 null이거나 비어있음: email={}", email);
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }
        
        String key = REFRESH_TOKEN_PREFIX + email;
        log.info("💾 [RefreshTokenService] 토큰 해시 생성 시작: email={}, key={}", email, key);
        
        String tokenHash = jwtTokenProvider.hashToken(refreshToken);
        log.info("✅ [RefreshTokenService] 토큰 해시 생성 완료: email={}, hashLength={}", 
                email, tokenHash != null ? tokenHash.length() : 0);
        
        // TTL을 초 단위로 변환 (밀리초 → 초)
        long ttlSeconds = refreshExpiration / 1000;
        log.info("⏰ [RefreshTokenService] TTL 설정: email={}, ttl={}초 ({}일)", 
                email, ttlSeconds, ttlSeconds / 86400);
        
        try {
            // Valkey에 저장
            redisTemplate.opsForValue().set(key, tokenHash, ttlSeconds, TimeUnit.SECONDS);
            log.info("✅ [RefreshTokenService] Valkey 저장 완료: email={}, key={}, ttl={}초", 
                    email, key, ttlSeconds);
            
            // 저장 확인 (즉시 조회)
            String savedValue = redisTemplate.opsForValue().get(key);
            if (savedValue != null && savedValue.equals(tokenHash)) {
                log.info("✅ [RefreshTokenService] 저장 검증 성공: email={}, key={}", email, key);
            } else {
                log.error("❌ [RefreshTokenService] 저장 검증 실패: email={}, key={}, savedValue={}", 
                        email, key, savedValue);
                throw new RuntimeException("Valkey 저장 검증 실패: 저장된 값이 일치하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("❌ [RefreshTokenService] Valkey 저장 중 예외 발생: email={}, key={}, error={}", 
                    email, key, e.getMessage(), e);
            throw new RuntimeException("Refresh Token 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Valkey에서 Refresh Token 조회
     * 
     * @param email 사용자 이메일
     * @return Refresh Token 해시값 (없으면 null)
     */
    public String getRefreshToken(String email) {
        if (email == null || email.isEmpty()) {
            log.error("❌ [RefreshTokenService] getRefreshToken: 이메일이 null이거나 비어있음");
            return null;
        }
        
        String key = REFRESH_TOKEN_PREFIX + email;
        log.debug("🔍 [RefreshTokenService] Valkey에서 토큰 조회 시작: key={}", key);
        
        try {
            String tokenHash = redisTemplate.opsForValue().get(key);
            if (tokenHash == null) {
                log.warn("⚠️ [RefreshTokenService] Valkey에 토큰이 없음: key={}", key);
            } else {
                log.debug("✅ [RefreshTokenService] Valkey에서 토큰 조회 성공: key={}, hashLength={}", 
                        key, tokenHash.length());
            }
            return tokenHash;
        } catch (Exception e) {
            log.error("❌ [RefreshTokenService] Valkey 조회 중 예외 발생: key={}, error={}", 
                    key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Refresh Token 검증 및 Rotation
     * 
     * 작동 원리:
     * 1. 클라이언트가 보낸 Refresh Token을 해시화
     * 2. Valkey에 저장된 해시값과 비교
     * 3. 일치하면 기존 토큰 삭제 후 새 토큰 저장 (Rotation)
     * 4. 불일치하면 탈취된 토큰으로 간주하고 예외 발생
     * 
     * @param email 사용자 이메일
     * @param oldRefreshToken 클라이언트가 보낸 Refresh Token
     * @param newRefreshToken 새로 발급할 Refresh Token
     * @return 검증 성공 여부
     * @throws InvalidTokenException 토큰 불일치 시
     */
    public boolean validateAndRotate(String email, String oldRefreshToken, String newRefreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String oldTokenHash = jwtTokenProvider.hashToken(oldRefreshToken);
        
        // Valkey에서 저장된 토큰 해시 조회
        String storedTokenHash = redisTemplate.opsForValue().get(key);
        
        if (storedTokenHash == null) {
            // 토큰이 Valkey에 없음 → 탈취된 토큰 또는 만료된 토큰
            log.error("🔒 [보안 경고] Valkey에 Refresh Token이 없음: email={}", email);
            throw new InvalidTokenException(
                "Refresh Token을 찾을 수 없습니다. 다시 로그인해주세요."
            );
        }
        
        if (!storedTokenHash.equals(oldTokenHash)) {
            // 토큰 불일치 → 탈취된 토큰으로 간주
            log.error("🔒 [보안 경고] Refresh Token 불일치: email={}", email);
            
            // 보안 강화: 해당 사용자의 모든 세션 무효화
            deleteRefreshToken(email);
            
            throw new InvalidTokenException(
                "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."
            );
        }
        
        // 토큰 일치 → Rotation 수행
        // 원자적 연산: 기존 토큰 삭제 → 새 토큰 저장
        log.debug("🔄 Refresh Token Rotation 시작: email={}", email);
        
        // 기존 토큰 삭제
        redisTemplate.delete(key);
        
        // 새 토큰 저장
        saveRefreshToken(email, newRefreshToken);
        
        log.info("✅ Refresh Token Rotation 완료: email={}", email);
        return true;
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * 
     * @param email 사용자 이메일
     */
    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
        log.debug("🗑️ Refresh Token 삭제 완료: email={}", email);
    }

    /**
     * 사용자의 모든 Refresh Token 삭제 (보안 강화용)
     * 
     * @param email 사용자 이메일
     */
    public void deleteAllRefreshTokens(String email) {
        deleteRefreshToken(email);
        log.info("🔒 모든 Refresh Token 삭제 완료: email={}", email);
    }
}

