package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_reviews_user_product", columnNames = {"user_id", "product_id"})
        },
        indexes = {
            @Index(name = "idx_reviews_product", columnList = "product_id"),
            @Index(name = "idx_reviews_user", columnList = "user_id"),
            @Index(name = "idx_reviews_approved", columnList = "product_id, is_approved"),
            @Index(name = "idx_reviews_created", columnList = "created_at DESC")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = true;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
