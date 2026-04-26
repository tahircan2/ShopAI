package com.shopai.dto.response;

import com.shopai.entity.Order;
import com.shopai.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponses {

    @Data @Builder
    public static class OrderResponse {
        private Long id;
        private String orderNumber;
        private String status;
        private String paymentStatus;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal shippingCost;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String couponCode;
        private AddressResponse shippingAddress;
        private List<OrderItemResponse> items;
        private String notes;
        private String paymentMethod;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private LocalDateTime createdAt;

        public static OrderResponse from(Order o) {
            return OrderResponse.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus().name())
                    .paymentStatus(o.getPaymentStatus().name())
                    .subtotal(o.getSubtotal())
                    .taxAmount(o.getTaxAmount())
                    .shippingCost(o.getShippingCost())
                    .discountAmount(o.getDiscountAmount())
                    .totalAmount(o.getTotalAmount())
                    .couponCode(o.getCouponCode())
                    .shippingAddress(o.getShippingAddress() != null
                            ? AddressResponse.from(o.getShippingAddress()) : null)
                    .items(o.getItems().stream().map(OrderItemResponse::from).toList())
                    .notes(o.getNotes())
                    .paymentMethod(o.getPaymentMethod())
                    .shippedAt(o.getShippedAt())
                    .deliveredAt(o.getDeliveredAt())
                    .createdAt(o.getCreatedAt())
                    .build();
        }
    }

    @Data @Builder
    public static class OrderSummaryResponse {
        private Long id;
        private String orderNumber;
        private String status;
        private BigDecimal totalAmount;
        private Integer itemCount;
        private LocalDateTime createdAt;
        private String userName;

        public static OrderSummaryResponse from(Order o) {
            String uName = "";
            if (o.getShippingAddress() != null) {
                uName = o.getShippingAddress().getFullName();
            } else if (o.getUser() != null) {
                uName = o.getUser().getFirstName() + " " + o.getUser().getLastName();
            }

            return OrderSummaryResponse.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus().name())
                    .totalAmount(o.getTotalAmount())
                    .itemCount(o.getItems().size())
                    .createdAt(o.getCreatedAt())
                    .userName(uName)
                    .build();
        }
    }

    @Data @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private Long sellerId;
        private String sellerName;
        private String description;
        private String color;
        private String size;
        private String primaryImageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static OrderItemResponse from(OrderItem i) {
            String primaryImg = null;
            if (i.getProduct() != null && i.getProduct().getImages() != null && !i.getProduct().getImages().isEmpty()) {
                primaryImg = i.getProduct().getImages().stream()
                        .filter(com.shopai.entity.ProductImage::getIsPrimary)
                        .map(com.shopai.entity.ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(i.getProduct().getImages().get(0).getImageUrl());
            }

            String color = null;
            String size = null;

            if (i.getVariantId() != null && i.getProduct() != null && i.getProduct().getVariants() != null) {
                var variant = i.getProduct().getVariants().stream()
                        .filter(v -> v.getId().equals(i.getVariantId()))
                        .findFirst();
                if (variant.isPresent()) {
                    color = variant.get().getColor();
                    size = variant.get().getSize();
                }
            }

            return OrderItemResponse.builder()
                    .id(i.getId())
                    .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                    .productName(i.getProductName())
                    .productSku(i.getProductSku())
                    .sellerId(i.getProduct() != null && i.getProduct().getSeller() != null 
                            ? i.getProduct().getSeller().getId() : null)
                    .sellerName(i.getProduct() != null && i.getProduct().getSeller() != null 
                            ? (i.getProduct().getSeller().getShopName() != null && !i.getProduct().getSeller().getShopName().isBlank()
                                ? i.getProduct().getSeller().getShopName()
                                : i.getProduct().getSeller().getFirstName() + " " + i.getProduct().getSeller().getLastName()) 
                            : null)
                    .description(i.getProduct() != null ? i.getProduct().getDescription() : null)
                    .color(color)
                    .size(size)
                    .primaryImageUrl(primaryImg)
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .totalPrice(i.getTotalPrice())
                    .build();
        }
    }
}
