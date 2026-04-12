package com.shopai.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequests {

    @Data
    public static class CreateProductRequest {
        @NotBlank(message = "Ürün adı zorunludur")
        @Size(max = 255)
        private String name;

        @Size(max = 255)
        private String slug;

        private String description;
        private String longDescription;

        @NotNull(message = "Fiyat zorunludur")
        @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat sıfırdan büyük olmalıdır")
        private BigDecimal price;

        private BigDecimal discountedPrice;

        @Min(value = 0, message = "Stok miktarı negatif olamaz")
        private Integer stockQuantity = 0;

        @Size(max = 100)
        private String sku;

        private Long categoryId;

        @Size(max = 100)
        private String brand;

        private Boolean isFeatured = false;
        private List<String> tags;

        @Size(max = 255)
        private String metaTitle;

        @Size(max = 500)
        private String metaDescription;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UpdateProductRequest extends CreateProductRequest {}

    @Data
    public static class ProductFilterRequest {
        private Long categoryId;
        private String categorySlug;   // Frontend sends category slug instead of ID
        private List<Long> categoryIds; // Populated by backend for recursive search
        private String q;              // Free-text keyword search
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private List<String> colors;
        private List<String> sizes;
        private String brand;
        private Double minRating;
        private Boolean inStock;
        private String sortBy = "createdAt";   // price, rating, ratingCount, createdAt
        private String sortDir = "desc";        // asc, desc
        private Integer page = 0;
        private Integer size = 20;
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "Puan zorunludur")
        @Min(1) @Max(5)
        private Integer rating;

        @Size(max = 255)
        private String title;

        @Size(max = 2000)
        private String comment;
    }

    @Data
    public static class CouponRequest {
        @NotBlank(message = "Kupon kodu zorunludur")
        @Size(max = 50)
        private String code;

        @NotBlank(message = "İndirim tipi zorunludur")
        private String discountType; // PERCENTAGE or FIXED

        @NotNull(message = "İndirim değeri zorunludur")
        @DecimalMin("0.01")
        private BigDecimal discountValue;

        private BigDecimal minOrderAmount;
        private Integer maxUses;

        @NotNull(message = "Başlangıç tarihi zorunludur")
        private java.time.LocalDateTime validFrom;

        @NotNull(message = "Bitiş tarihi zorunludur")
        private java.time.LocalDateTime validUntil;
    }

    @Data
    public static class CategoryRequest {
        @NotBlank(message = "Kategori adı zorunludur")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Slug zorunludur")
        @Size(max = 100)
        private String slug;

        private String description;
        
        private Long parentId;
        
        private String imageUrl;
        
        private Boolean isActive = true;
        
        private Integer sortOrder = 0;
    }
}
