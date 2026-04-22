package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
        indexes = {
            @Index(name = "idx_audit_logs_user", columnList = "user_id"),
            @Index(name = "idx_audit_logs_action", columnList = "action"),
            @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
            @Index(name = "idx_audit_logs_created", columnList = "created_at DESC"),
            @Index(name = "idx_audit_logs_ip", columnList = "ip_address, created_at DESC")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "JSON")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "JSON")
    private String newData;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // ── Agentic UI Control — AI kaynaklı işlem bayrağı ──

    /**
     * Bu işlem AI agent tarafından mı yapıldı?
     */
    @Column(name = "is_ai_action", nullable = false)
    @Builder.Default
    private Boolean isAiAction = false;

    /**
     * AI işlemi ise ilgili AgentTransaction ID'si.
     */
    @Column(name = "agent_transaction_id")
    private Long agentTransactionId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
