package com.kh.magamGG.domain.auth.repository;

import com.kh.magamGG.domain.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시로 활성 토큰 조회
     * PESSIMISTIC_WRITE 락으로 동시성 문제 방지
     * 엔티티의 isRevoked() 메서드 사용을 위해 활성 토큰만 조회하는 메서드 제공
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByRefreshTokenHashAndRefreshTokenIsRevoked(String refreshTokenHash, String isRevoked);

    /**
     * 토큰 해시로 활성 토큰 조회 (편의 메서드)
     * PESSIMISTIC_WRITE 락으로 동시성 문제 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default Optional<RefreshToken> findActiveByRefreshTokenHash(String refreshTokenHash) {
        return findByRefreshTokenHashAndRefreshTokenIsRevoked(refreshTokenHash, "F");
    }

    /**
     * 토큰 해시로 revoked 토큰 조회 (재사용 공격 감지용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default Optional<RefreshToken> findRevokedByRefreshTokenHash(String refreshTokenHash) {
        return findByRefreshTokenHashAndRefreshTokenIsRevoked(refreshTokenHash, "T");
    }

    /**
     * 토큰 패밀리로 모든 토큰 조회 (재사용 공격 방어용)
     */
    List<RefreshToken> findByRefreshTokenFamily(String refreshTokenFamily);

    /**
     * 만료된 Refresh Token 삭제
     * 스케줄러에서 사용
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.refreshTokenExpiresAt < :now")
    void deleteByRefreshTokenExpiresAtBefore(LocalDateTime now);
}

