package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kullanıcı AI Tercihleri — Kullanıcının AI'a verdiği izinler ve tercihler.
 *
 * - Onaysız işlem yapılabilir mi?
 * - Hangi işlem tipleri için otomatik onay var?
 * - Varsayılan adres ve ödeme yöntemi kullanılsın mı?
 * - Günlük/saatlik işlem limitleri
 */
@Entity
@Table(name = "user_ai_preferences",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_user_ai_pref_user", columnNames = "user_id")
        },
        indexes = {
            @Index(name = "idx_user_ai_pref_user", columnList = "user_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAiPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Otomatik onay: true ise belirli koşullar altında kullanıcıya sormadan işlem yapılabilir.
     */
    @Column(name = "auto_approve_enabled", nullable = false)
    @Builder.Default
    private Boolean autoApproveEnabled = false;

    /**
     * Otomatik onay maksimum tutarı — bu tutarın altındaki siparişler onaysız oluşturulabilir.
     * null ise otomatik onay kapalı (autoApproveEnabled false ise bu alan görmezden gelinir).
     */
    @Column(name = "auto_approve_max_amount", precision = 10, scale = 2)
    private BigDecimal autoApproveMaxAmount;

    /**
     * Otomatik onay verilen işlem tipleri listesi.
     * JSON array: ["CART_MODIFY", "COUPON_APPLY"]
     * null veya boş ise hiçbir tip için otomatik onay yok.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_approve_categories", columnDefinition = "JSON")
    private String autoApproveCategories;

    /**
     * Checkout sırasında varsayılan adresi otomatik kullan.
     */
    @Column(name = "use_default_address", nullable = false)
    @Builder.Default
    private Boolean useDefaultAddress = true;

    /**
     * Checkout sırasında varsayılan ödeme yöntemini otomatik kullan.
     */
    @Column(name = "use_default_payment", nullable = false)
    @Builder.Default
    private Boolean useDefaultPayment = true;

    /**
     * Kullanıcı başına günlük maksimum AI işlem sayısı.
     */
    @Column(name = "daily_transaction_limit", nullable = false)
    @Builder.Default
    private Integer dailyTransactionLimit = 10;

    /**
     * AI ile oluşturulabilecek maksimum sipariş tutarı (TL).
     */
    @Column(name = "max_order_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxOrderAmount = new BigDecimal("5000.00");

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
