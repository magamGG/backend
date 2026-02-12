package com.kh.magamGG.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "REFRESH_TOKEN", indexes = {
    @Index(name = "idx_token_hash", columnList = "REFRESH_TOKEN_HASH"),
    @Index(name = "idx_member_no", columnList = "MEMBER_NO"),
    @Index(name = "idx_token_family", columnList = "REFRESH_TOKEN_FAMILY")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFRESH_TOKEN_ID")
    private Long refreshTokenId;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "REFRESH_TOKEN_HASH", nullable = false, length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "REFRESH_TOKEN_FAMILY", nullable = false, length = 36)
    private String refreshTokenFamily;

    @Column(name = "REFRESH_TOKEN_IS_REVOKED", nullable = false, length = 1)
    @Builder.Default
    private String refreshTokenIsRevoked = "F"; // T / F

    @Column(name = "REFRESH_TOKEN_EXPIRES_AT", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "REFRESH_TOKEN_CREATED_AT", nullable = false, 
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime refreshTokenCreatedAt = LocalDateTime.now();

    @Column(name = "REFRESH_TOKEN_UPDATED_AT",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime refreshTokenUpdatedAt = LocalDateTime.now();

    @Column(name = "REFRESH_TOKEN_LAST_USED_AT")
    private LocalDateTime refreshTokenLastUsedAt;

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(refreshTokenExpiresAt);
    }

    /**
     * 토큰 무효화 여부 확인
     */
    public boolean isRevoked() {
        return "T".equalsIgnoreCase(this.refreshTokenIsRevoked);
    }

    /**
     * 토큰 무효화 처리
     */
    public void revoke() {
        this.refreshTokenIsRevoked = "T";
        this.refreshTokenUpdatedAt = LocalDateTime.now();
    }

    /**
     * 토큰 사용 시간 업데이트
     */
    public void updateLastUsedAt() {
        this.refreshTokenLastUsedAt = LocalDateTime.now();
        this.refreshTokenUpdatedAt = LocalDateTime.now();
    }
}

