package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions",
        indexes = {
            @Index(name = "idx_user_sessions_user", columnList = "user_id"),
            @Index(name = "idx_user_sessions_expires", columnList = "expires_at"),
            @Index(name = "idx_user_sessions_active", columnList = "user_id, is_active")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token_hash", nullable = false, length = 255)
    private String sessionTokenHash;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
