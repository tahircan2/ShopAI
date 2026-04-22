package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent İşlem Adımları — Çok adımlı bir işlemin her adımını ayrı satır olarak tutar.
 * Hangi üst işleme ait olduğu (foreign key), adım sırası, adım tipi,
 * başarı/hata durumu, API yanıtının JSON özeti kaydedilir.
 */
@Entity
@Table(name = "agent_transaction_steps",
        indexes = {
            @Index(name = "idx_agent_step_tx", columnList = "transaction_id"),
            @Index(name = "idx_agent_step_tx_order", columnList = "transaction_id, step_order")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentTransactionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private AgentTransaction transaction;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false,
            columnDefinition = "ENUM('VALIDATE_CART','CHECK_STOCK','APPLY_COUPON','SELECT_ADDRESS','CREATE_ORDER','CLEAR_CART','VALIDATE_PAYMENT','PRE_VALIDATE')")
    private StepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('PENDING','IN_PROGRESS','COMPLETED','FAILED','ROLLED_BACK','SKIPPED')")
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "step_description", length = 500)
    private String stepDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "JSON")
    private Map<String, Object> requestData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "JSON")
    private Map<String, Object> responseData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Bu adım geri alınabilir mi?
     * Sepete ekleme → geri alınabilir (çıkar)
     * Kupon uygulama → geri alınabilir (kaldır)
     * Sipariş oluşturma → koşullu (PENDING ise iptal edilebilir)
     */
    @Column(name = "is_rollbackable", nullable = false)
    @Builder.Default
    private Boolean isRollbackable = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enum Definitions ──

    public enum StepType {
        VALIDATE_CART, CHECK_STOCK, APPLY_COUPON, SELECT_ADDRESS,
        CREATE_ORDER, CLEAR_CART, VALIDATE_PAYMENT, PRE_VALIDATE
    }

    public enum StepStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK, SKIPPED
    }
}
