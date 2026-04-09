package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_product_variants_sku", columnNames = "sku_variant")
        },
        indexes = {
            @Index(name = "idx_product_variants_product", columnList = "product_id"),
            @Index(name = "idx_product_variants_color", columnList = "color"),
            @Index(name = "idx_product_variants_size", columnList = "size")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String color;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(length = 20)
    private String size;

    @Column(name = "sku_variant", unique = true, length = 100)
    private String skuVariant;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "price_modifier", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;
}
