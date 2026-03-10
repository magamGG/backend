package com.kh.magamGG.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Valkey(Redis 호환) 설정
 * 
 * 주요 기능:
 * - Valkey 서버 연결 설정 (localhost:6379)
 * - RedisTemplate 빈 등록 (Refresh Token 저장용)
 * - RedisCacheManager 설정 (캐싱용)
 * 
 * 보안 고려사항:
 * - Refresh Token은 평문이 아닌 해시값으로 저장 (JwtTokenProvider.hashToken() 사용)
 * - TTL은 application.yaml의 jwt.refresh-expiration 값과 동기화
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Valkey(Redis) 연결 팩토리 생성
     * Lettuce를 사용하여 비동기, 논블로킹 연결 제공
     * 
     * 주의: Valkey 서버가 실행되지 않아도 빈 생성은 성공하지만,
     * 실제 연결 시도 시 예외가 발생할 수 있습니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            log.info("🔌 [RedisConfig] Valkey 연결 팩토리 생성 시작: host={}, port={}", redisHost, redisPort);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            log.info("✅ [RedisConfig] Valkey 연결 팩토리 생성 완료");
            return factory;
        } catch (Exception e) {
            log.error("❌ [RedisConfig] Valkey 연결 팩토리 생성 실패: {}", e.getMessage(), e);
            // 빈 생성은 성공시키되, 실제 연결은 나중에 시도
            throw new RuntimeException("Valkey 연결 팩토리 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * RedisTemplate 빈 등록
     * Refresh Token 저장/조회에 사용
     * 
     * 직렬화 설정:
     * - Key: String (예: "RT:user@example.com")
     * - Value: String (Refresh Token 해시값)
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key 직렬화: String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화: String
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisCacheManager 설정
     * 애플리케이션 캐싱(leaveBalance, agencyInfo)에 사용
     * 
     * 캐시별 TTL 설정 가능:
     * - leaveBalance: 1시간
     * - agencyInfo: 1시간
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // 기본 TTL: 1시간
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();  // null 값 캐싱 방지

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // leaveBalance 캐시 설정
        cacheConfigurations.put("leaveBalance", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // agencyInfo 캐시 설정
        cacheConfigurations.put("agencyInfo", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}

