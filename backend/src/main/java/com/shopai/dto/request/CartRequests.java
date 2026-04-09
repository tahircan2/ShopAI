package com.shopai.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CartRequests {

    @Data
    public static class AddToCartRequest {
        @NotNull(message = "Ürün ID zorunludur")
        private Long productId;

        private Long variantId;

        @NotNull
        @Min(value = 1, message = "Adet en az 1 olmalıdır")
        private Integer quantity;
    }

    @Data
    public static class UpdateQuantityRequest {
        @NotNull
        @Min(value = 1, message = "Adet en az 1 olmalıdır")
        private Integer quantity;
    }

    @Data
    public static class ApplyCouponRequest {
        @NotNull(message = "Kupon kodu zorunludur")
        private String code;
    }
}
