package com.kh.magamGG.domain.auth.repository;

import com.kh.magamGG.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시로 활성 토큰 조회
     */
    Optional<RefreshToken> findByRefreshTokenHashAndRefreshTokenIsRevokedFalse(String refreshTokenHash);

    /**
     * 토큰 패밀리로 모든 토큰 조회 (재사용 공격 방어용)
     */
    List<RefreshToken> findByRefreshTokenFamily(String refreshTokenFamily);
}

