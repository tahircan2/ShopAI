package com.shopai.dto.response;

import com.shopai.entity.Cart;
import com.shopai.entity.CartItem;
import com.shopai.entity.ProductImage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Data @Builder
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingCost;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String appliedCoupon;
    private boolean freeShipping;

    // KDV oranı %18, ücretsiz kargo limiti 500 TL
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("29.90");

    public static CartResponse from(Cart cart, String appliedCoupon, BigDecimal discountAmount) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discount).max(BigDecimal.ZERO);

        boolean freeShipping = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
        BigDecimal shipping = freeShipping ? BigDecimal.ZERO : SHIPPING_COST;

        BigDecimal tax = afterDiscount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = afterDiscount.add(shipping).add(tax);

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(tax)
                .shippingCost(shipping)
                .discountAmount(discount.setScale(2, RoundingMode.HALF_UP))
                .total(total.setScale(2, RoundingMode.HALF_UP))
                .appliedCoupon(appliedCoupon)
                .freeShipping(freeShipping)
                .build();
    }

    @Data @Builder
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSlug;
        private String primaryImageUrl;
        private Long variantId;
        private String variantColor;
        private String variantSize;
        private Integer quantity;
        private BigDecimal priceAtAdd;
        private BigDecimal currentPrice;
        private BigDecimal lineTotal;

        public static CartItemResponse from(CartItem item) {
            String primaryImg = item.getProduct().getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(item.getProduct().getImages().isEmpty()
                            ? null : item.getProduct().getImages().get(0).getImageUrl());

            BigDecimal currentPrice = item.getProduct().getEffectivePrice();
            if (item.getVariant() != null) {
                currentPrice = currentPrice.add(item.getVariant().getPriceModifier());
            }
            BigDecimal lineTotal = item.getPriceAtAdd()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productSlug(item.getProduct().getSlug())
                    .primaryImageUrl(primaryImg)
                    .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                    .variantColor(item.getVariant() != null ? item.getVariant().getColor() : null)
                    .variantSize(item.getVariant() != null ? item.getVariant().getSize() : null)
                    .quantity(item.getQuantity())
                    .priceAtAdd(item.getPriceAtAdd())
                    .currentPrice(currentPrice)
                    .lineTotal(lineTotal)
                    .build();
        }
    }
}
