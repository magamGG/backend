package com.kh.magamGG.domain.auth.service;

import com.kh.magamGG.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh Token 정리 스케줄러
 * 만료된 토큰을 주기적으로 삭제하여 DB 용량 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 Refresh Token 자동 정리
     * 매일 새벽 3시에 실행
     * 대량 데이터 시에도 안정적으로 동작하도록 @Modifying(clearAutomatically = true) 사용
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Transactional
    public void deleteExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 삭제 전 개수 확인 (로깅용)
            long countBefore = refreshTokenRepository.count();
            
            // 만료된 토큰 삭제 (clearAutomatically = true로 설정되어 있어 자동으로 영속성 컨텍스트 클리어)
            refreshTokenRepository.deleteByRefreshTokenExpiresAtBefore(now);
            
            // 삭제 후 개수 확인
            long countAfter = refreshTokenRepository.count();
            long deletedCount = countBefore - countAfter;
            
            if (deletedCount > 0) {
                log.info("만료된 Refresh Token 정리 완료: {}개 토큰 삭제 (삭제 전: {}, 삭제 후: {})", 
                        deletedCount, countBefore, countAfter);
            } else {
                log.debug("만료된 Refresh Token 정리 완료: 삭제할 토큰 없음");
            }
        } catch (Exception e) {
            log.error("만료된 Refresh Token 정리 중 오류 발생", e);
        }
    }
}

