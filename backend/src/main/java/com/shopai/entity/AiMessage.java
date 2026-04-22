package com.shopai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_messages",
        indexes = {
            @Index(name = "idx_ai_messages_conversation", columnList = "conversation_id"),
            @Index(name = "idx_ai_messages_injection", columnList = "is_injection_detected"),
            @Index(name = "idx_ai_messages_created", columnList = "created_at DESC")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('user','assistant','system')")
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "agent_type", length = 50)
    private String agentType;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_data", columnDefinition = "JSON")
    private String actionData;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "processing_ms")
    private Integer processingMs;

    @Column(name = "is_injection_detected", nullable = false)
    @Builder.Default
    private Boolean isInjectionDetected = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageRole {
        user, assistant, system
    }
}
