package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Bekleyen Onay — AI'ın kullanıcıdan onay beklediği işlemleri geçici olarak tutar.
 *
 * GÜVENLİK KURALLARI:
 * - approvalToken UUID formatında, tek kullanımlık, kısa ömürlü (10 dakika)
 * - planHash: İşlem planının SHA-256 hash'i. Onay anında plan hash'i yeniden hesaplanır
 *   ve kaydedilen hash ile karşılaştırılır. Farklılık → PLAN_TAMPERED hatası.
 * - Token kullanıldıktan sonra hemen APPROVED/REJECTED olarak işaretlenir, tekrar kullanılamaz.
 */
@Entity
@Table(name = "pending_approvals",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_pending_approval_token", columnNames = "approval_token")
        },
        indexes = {
            @Index(name = "idx_pending_approval_token", columnList = "approval_token"),
            @Index(name = "idx_pending_approval_user", columnList = "user_id"),
            @Index(name = "idx_pending_approval_status", columnList = "status"),
            @Index(name = "idx_pending_approval_expires", columnList = "expires_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PendingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "approval_token", unique = true, nullable = false, length = 255)
    private String approvalToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private AgentTransaction transaction;

    /**
     * İşlem planının SHA-256 hash değeri.
     * Kullanıcıya gösterilen plan ile backend'in çalıştırdığı planın aynı olduğu doğrulanır.
     */
    @Column(name = "plan_hash", nullable = false, length = 64)
    private String planHash;

    /**
     * İşlem planının tam JSON içeriği.
     * {steps: [{order, type, description, isRollbackable}], summary: "...", estimatedDuration: "..."}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan_data", nullable = false, columnDefinition = "JSON")
    private String planData;

    @Column(name = "agent_type", length = 50)
    private String agentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('PENDING','APPROVED','REJECTED','EXPIRED')")
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * Son geçerlilik tarihi — oluşturma zamanı + 10 dakika.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Helper Methods ──

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING && !isExpired();
    }

    public boolean canBeUsed() {
        return status == ApprovalStatus.APPROVED && !isExpired();
    }

    // ── Enum ──

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, EXPIRED
    }
}
