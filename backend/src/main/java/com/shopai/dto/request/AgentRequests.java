package com.shopai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent işlemleri için request DTO'ları.
 */
public class AgentRequests {

    // ── Onay Sistemi ──

    @Data
    public static class CreateApprovalRequest {
        @NotBlank(message = "Plan verisi boş olamaz")
        private String planData;

        @NotBlank(message = "Agent tipi boş olamaz")
        @Size(max = 50)
        private String agentType;

        @NotBlank(message = "Session ID boş olamaz")
        private String sessionId;
    }

    @Data
    public static class ApprovalActionRequest {
        // Token URL path'inden gelir, body içinde ek alan gerekirse buraya eklenir
    }

    // ── Quick Checkout ──

    @Data
    public static class QuickCheckoutValidateRequest {
        private Long shippingAddressId;
        private String couponCode;
        private String paymentMethod;
    }

    @Data
    public static class QuickCheckoutExecuteRequest {
        @NotBlank(message = "Onay token'ı zorunludur")
        private String approvalToken;

        @NotBlank(message = "Plan verisi zorunludur")
        private String planData;

        @NotNull(message = "Teslimat adresi zorunludur")
        private Long shippingAddressId;

        private String couponCode;

        @NotBlank(message = "Ödeme yöntemi zorunludur")
        private String paymentMethod;

        private String notes;
    }

    // ── AI Tercihleri ──

    @Data
    public static class UpdateAiPreferenceRequest {
        private Boolean autoApproveEnabled;
        private BigDecimal autoApproveMaxAmount;
        private List<String> autoApproveCategories;
        private Boolean useDefaultAddress;
        private Boolean useDefaultPayment;
    }

    // ── Geri Bildirim ──

    @Data
    public static class AgentFeedbackRequest {
        @NotNull(message = "İşlem ID'si zorunludur")
        private Long transactionId;

        @NotNull(message = "Puan zorunludur")
        private Integer score; // 1-5

        @Size(max = 500)
        private String feedbackText;
    }
}
