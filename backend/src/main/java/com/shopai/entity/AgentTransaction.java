package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Agent İşlem Geçmişi — AI'ın gerçekleştirdiği her çok adımlı işlemi kayıt altına alır.
 * Hangi kullanıcının, hangi zaman, hangi adımları tamamladığı, hangisinin başarısız olduğu,
 * toplam işlem süresi ve kullanılan token sayısı tutulur.
 */
@Entity
@Table(name = "agent_transactions",
        indexes = {
            @Index(name = "idx_agent_tx_user", columnList = "user_id"),
            @Index(name = "idx_agent_tx_session", columnList = "session_id"),
            @Index(name = "idx_agent_tx_status", columnList = "status"),
            @Index(name = "idx_agent_tx_created", columnList = "created_at DESC"),
            @Index(name = "idx_agent_tx_user_date", columnList = "user_id, created_at DESC")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,
            columnDefinition = "ENUM('CHECKOUT','CART_MODIFY','NAVIGATE','COUPON_APPLY','ORDER_QUERY','MULTI_STEP')")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('PENDING','IN_PROGRESS','AWAITING_APPROVAL','COMPLETED','FAILED','CANCELLED','ROLLED_BACK')")
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "total_steps", nullable = false)
    @Builder.Default
    private Integer totalSteps = 0;

    @Column(name = "completed_steps", nullable = false)
    @Builder.Default
    private Integer completedSteps = 0;

    @Column(name = "failed_step")
    private Integer failedStep;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Kullanıcı geri bildirimi — AI işlemi sonrası.
     * 1 = olumsuz, 5 = çok olumlu, null = henüz verilmedi
     */
    @Column(name = "feedback_score")
    private Integer feedbackScore;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    /**
     * Oluşturulan sipariş numarası (varsa) — checkout işlemlerinde.
     */
    @Column(name = "result_order_number", length = 20)
    private String resultOrderNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private java.util.List<AgentTransactionStep> steps = new java.util.ArrayList<>();

    // ── Enum Definitions ──

    public enum TransactionType {
        CHECKOUT, CART_MODIFY, NAVIGATE, COUPON_APPLY, ORDER_QUERY, MULTI_STEP
    }

    public enum TransactionStatus {
        PENDING, IN_PROGRESS, AWAITING_APPROVAL, COMPLETED, FAILED, CANCELLED, ROLLED_BACK
    }
}
