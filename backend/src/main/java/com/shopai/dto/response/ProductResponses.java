package com.shopai.dto.response;

import com.shopai.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductResponses {

    @Data
    @Builder
    public static class ProductResponse {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String longDescription;
        private BigDecimal price;
        private BigDecimal discountedPrice;
        private BigDecimal effectivePrice;
        private Integer stockQuantity;
        private String sku;
        private CategoryResponse category;
        private String brand;
        private BigDecimal ratingAvg;
        private Integer ratingCount;
        private Boolean isActive;
        private Boolean isFeatured;
        private List<String> tags;
        private List<ProductImageResponse> images;
        private List<ProductVariantResponse> variants;
        private Long sellerId;
        private String sellerName;
        private LocalDateTime createdAt;

        public static ProductResponse from(Product p) {
            return ProductResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .slug(p.getSlug())
                    .description(p.getDescription())
                    .longDescription(p.getLongDescription())
                    .price(p.getPrice())
                    .discountedPrice(p.getDiscountedPrice())
                    .effectivePrice(p.getEffectivePrice())
                    .stockQuantity(p.getStockQuantity())
                    .sku(p.getSku())
                    .category(p.getCategory() != null ? CategoryResponse.from(p.getCategory()) : null)
                    .brand(p.getBrand())
                    .ratingAvg(p.getRatingAvg())
                    .ratingCount(p.getRatingCount())
                    .isActive(p.getIsActive())
                    .isFeatured(p.getIsFeatured())
                    .tags(p.getTags())
                    .images(p.getImages().stream().map(ProductImageResponse::from).toList())
                    .variants(p.getVariants().stream().map(ProductVariantResponse::from).toList())
                    .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                    .sellerName(p.getSeller() != null ? 
                            (p.getSeller().getShopName() != null && !p.getSeller().getShopName().isBlank() ? 
                                    p.getSeller().getShopName() : 
                                    p.getSeller().getFirstName() + " " + p.getSeller().getLastName()) 
                            : null)
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ProductSummaryResponse {
        private Long id;
        private String name;
        private String slug;
        private String sku;
        private String description;
        private BigDecimal price;
        private BigDecimal discountedPrice;
        private BigDecimal effectivePrice;
        private Integer stockQuantity;
        private String brand;
        private BigDecimal ratingAvg;
        private Integer ratingCount;
        private Boolean isFeatured;
        private Boolean isActive;
        private String primaryImageUrl;
        private Long sellerId;
        private String sellerName;
        // Kategori bilgileri
        private Long categoryId;
        private String categoryName;
        private String categorySlug;
        private Boolean hasVariants;

        public static ProductSummaryResponse from(Product p) {
            String primaryImg = p.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

            return ProductSummaryResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .slug(p.getSlug())
                    .sku(p.getSku())
                    .description(p.getDescription())
                    .price(p.getPrice())
                    .discountedPrice(p.getDiscountedPrice())
                    .effectivePrice(p.getEffectivePrice())
                    .stockQuantity(p.getStockQuantity())
                    .brand(p.getBrand())
                    .ratingAvg(p.getRatingAvg())
                    .ratingCount(p.getRatingCount())
                    .isFeatured(p.getIsFeatured())
                    .isActive(p.getIsActive())
                    .primaryImageUrl(primaryImg)
                    .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                    .sellerName(p.getSeller() != null ? 
                            (p.getSeller().getShopName() != null && !p.getSeller().getShopName().isBlank() ? 
                                    p.getSeller().getShopName() : 
                                    p.getSeller().getFirstName() + " " + p.getSeller().getLastName()) 
                            : null)
                    .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                    .hasVariants(p.getVariants() != null && !p.getVariants().isEmpty())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ProductImageResponse {
        private Long id;
        private String imageUrl;
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;

        public static ProductImageResponse from(ProductImage i) {
            return ProductImageResponse.builder()
                    .id(i.getId())
                    .imageUrl(i.getImageUrl())
                    .altText(i.getAltText())
                    .isPrimary(i.getIsPrimary())
                    .sortOrder(i.getSortOrder())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ProductVariantResponse {
        private Long id;
        private String color;
        private String colorHex;
        private String size;
        private String skuVariant;
        private Integer stockQuantity;
        private BigDecimal priceModifier;

        public static ProductVariantResponse from(ProductVariant v) {
            return ProductVariantResponse.builder()
                    .id(v.getId())
                    .color(v.getColor())
                    .colorHex(v.getColorHex())
                    .size(v.getSize())
                    .skuVariant(v.getSkuVariant())
                    .stockQuantity(v.getStockQuantity())
                    .priceModifier(v.getPriceModifier())
                    .build();
        }
    }

    @Data
    @Builder
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String imageUrl;
        private Boolean isActive;
        private Integer sortOrder;
        private List<CategoryResponse> children;

        public static CategoryResponse from(Category c) {
            return CategoryResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .description(c.getDescription())
                    .imageUrl(c.getImageUrl())
                    .isActive(c.getIsActive())
                    .sortOrder(c.getSortOrder())
                    .children(c.getChildren() != null
                            ? c.getChildren().stream()
                                    .filter(Category::getIsActive)
                                    .map(CategoryResponse::from)
                                    .toList()
                            : List.of())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ReviewResponse {
        private Long id;
        private Long userId;
        private String userFullName;
        private Long productId;
        private String productSlug;
        private String productName;
        private Integer rating;
        private String title;
        private String comment;
        private Boolean isVerifiedPurchase;
        private Integer helpfulCount;
        private LocalDateTime createdAt;

        public static ReviewResponse from(Review r) {
            return ReviewResponse.builder()
                    .id(r.getId())
                    .userId(r.getUser().getId())
                    .userFullName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                    .productId(r.getProduct().getId())
                    .productSlug(r.getProduct().getSlug())
                    .productName(r.getProduct().getName())
                    .rating(r.getRating())
                    .title(r.getTitle())
                    .comment(r.getComment())
                    .isVerifiedPurchase(r.getIsVerifiedPurchase())
                    .helpfulCount(r.getHelpfulCount())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ProductReviewListResponse {
        private ProductSummaryResponse product;
        private List<ReviewResponse> reviews;
    }
}
