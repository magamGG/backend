package com.kh.magamGG.domain.refresh_token.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "REFRESH_TOKEN")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @Column(name = "REFRESH_TOKEN_HASH", nullable = false, length = 64)
    private String refreshTokenHash; // UNIQUE (DDL에서 인덱스 권장)

    @Column(name = "REFRESH_TOKEN_FAMILY", nullable = false, length = 36)
    private String refreshTokenFamily;

    @Column(name = "REFRESH_TOKEN_EXPIRES_AT", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "REFRESH_TOKEN_IS_REVOKED", nullable = false, length = 1)
    private String refreshTokenIsRevoked; // T / F

    @Column(
        name = "REFRESH_TOKEN_CREATED_AT",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime refreshTokenCreatedAt;

    @Column(
        name = "REFRESH_TOKEN_UPDATED_AT",
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private LocalDateTime refreshTokenUpdatedAt;

    @Column(name = "REFRESH_TOKEN_LAST_USED_AT")
    private LocalDateTime refreshTokenLastUsedAt;

    public boolean isRevoked() {
        return "T".equalsIgnoreCase(this.refreshTokenIsRevoked);
    }

    public void revoke() {
        this.refreshTokenIsRevoked = "T";
        this.refreshTokenUpdatedAt = LocalDateTime.now();
    }
}


