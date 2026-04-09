package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_coupons_code", columnNames = "code")
        },
        indexes = {
            @Index(name = "idx_coupons_code", columnList = "code"),
            @Index(name = "idx_coupons_validity", columnList = "is_active, valid_from, valid_until")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, columnDefinition = "ENUM('PERCENTAGE','FIXED')")
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean isValid(BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (!isActive) return false;
        if (now.isBefore(validFrom) || now.isAfter(validUntil)) return false;
        if (maxUses != null && usedCount >= maxUses) return false;
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) return false;
        return true;
    }

    public enum DiscountType {
        PERCENTAGE, FIXED
    }
}
