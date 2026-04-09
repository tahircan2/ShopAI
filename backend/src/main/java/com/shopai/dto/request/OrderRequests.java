package com.shopai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class OrderRequests {

    @Data
    public static class CreateOrderRequest {
        @NotNull(message = "Teslimat adresi zorunludur")
        private Long shippingAddressId;

        private String notes;
        private String paymentMethod;
    }

    @Data
    public static class UpdateOrderStatusRequest {
        @NotNull(message = "Sipariş durumu zorunludur")
        private String status;
    }
}
