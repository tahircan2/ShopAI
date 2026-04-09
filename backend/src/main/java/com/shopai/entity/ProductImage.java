package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images",
        indexes = {
            @Index(name = "idx_product_images_product", columnList = "product_id"),
            @Index(name = "idx_product_images_primary", columnList = "product_id, is_primary")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
