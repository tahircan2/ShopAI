package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_refresh_tokens_hash", columnNames = "token_hash")
        },
        indexes = {
            @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
            @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
            @Index(name = "idx_refresh_tokens_revoked", columnList = "revoked_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", unique = true, nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
