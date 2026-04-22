package com.shopai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversations",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_ai_conversations_session", columnNames = "session_id")
        },
        indexes = {
            @Index(name = "idx_ai_conversations_user", columnList = "user_id"),
            @Index(name = "idx_ai_conversations_session", columnList = "session_id"),
            @Index(name = "idx_ai_conversations_last_msg", columnList = "last_message_at DESC")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // NULL = anonim kullanıcı

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // ── Agentic UI Control — İşlem ve onay durumu takibi ──

    /**
     * Bu konuşmada aktif olan agent işlem ID'si.
     * Null ise şu an devam eden bir çok adımlı işlem yok.
     */
    @Column(name = "active_transaction_id")
    private Long activeTransactionId;

    /**
     * Bu konuşmada bekleyen onay ID'si.
     * Null ise şu an onay bekleyen bir işlem yok.
     */
    @Column(name = "pending_approval_id")
    private Long pendingApprovalId;

    @Column(name = "message_count", nullable = false)
    @Builder.Default
    private Integer messageCount = 0;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AiMessage> messages = new ArrayList<>();
}
