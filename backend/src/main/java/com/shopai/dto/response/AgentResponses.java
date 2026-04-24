package com.shopai.dto.response;

import com.shopai.entity.AgentTransaction.TransactionStatus;
import com.shopai.entity.AgentTransaction.TransactionType;
import com.shopai.entity.AgentTransactionStep.StepStatus;
import com.shopai.entity.AgentTransactionStep.StepType;
import com.shopai.entity.PendingApproval.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent işlemleri için response DTO'ları.
 */
public class AgentResponses {

    // ── Onay Yanıtları ──

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApprovalResponse {
        private Long id;
        private String approvalToken;
        private String planData;
        private String planHash;
        private String agentType;
        private ApprovalStatus status;
        private LocalDateTime expiresAt;
        private LocalDateTime respondedAt;
        private LocalDateTime createdAt;
        private long remainingSeconds;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApprovalActionResponse {
        private String status; // APPROVED, REJECTED, EXPIRED
        private String message;
        private Long transactionId;
    }

    // ── İşlem Durumu ──

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private TransactionType transactionType;
        private TransactionStatus status;
        private Integer totalSteps;
        private Integer completedSteps;
        private Integer failedStep;
        private Long totalDurationMs;
        private String errorMessage;
        private String resultOrderNumber;
        private Integer feedbackScore;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<TransactionStepResponse> steps;

        public static TransactionResponse fromEntity(com.shopai.entity.AgentTransaction entity) {
            if (entity == null) return null;
            return TransactionResponse.builder()
                    .id(entity.getId())
                    .transactionType(entity.getTransactionType())
                    .status(entity.getStatus())
                    .totalSteps(entity.getTotalSteps())
                    .completedSteps(entity.getCompletedSteps())
                    .failedStep(entity.getFailedStep())
                    .totalDurationMs(entity.getTotalDurationMs())
                    .errorMessage(entity.getErrorMessage())
                    .resultOrderNumber(entity.getResultOrderNumber())
                    .feedbackScore(entity.getFeedbackScore())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .steps(null) // Adımlar lazy load edildiği için N+1 veya LazyInitException yapmamak adına listede getirilmez.
                    .build();
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionStepResponse {
        private Long id;
        private Integer stepOrder;
        private StepType stepType;
        private StepStatus status;
        private String stepDescription;
        private String errorMessage;
        private Long durationMs;
        private Boolean isRollbackable;
        private LocalDateTime createdAt;

        public static TransactionStepResponse fromEntity(com.shopai.entity.AgentTransactionStep entity) {
            if (entity == null) return null;
            return TransactionStepResponse.builder()
                    .id(entity.getId())
                    .stepOrder(entity.getStepOrder())
                    .stepType(entity.getStepType())
                    .status(entity.getStatus())
                    .stepDescription(entity.getStepDescription())
                    .errorMessage(entity.getErrorMessage())
                    .durationMs(entity.getDurationMs())
                    .isRollbackable(entity.getIsRollbackable())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    // ── Quick Checkout ──

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuickCheckoutValidationResponse {
        private boolean valid;
        private List<ValidationIssue> issues;
        private List<String> warnings;
        private CartSummary cartSummary;
        private List<ApplicableCoupon> applicableCoupons;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidationIssue {
        private String field;
        private String message;
        private String severity; // ERROR, WARNING
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartSummary {
        private int itemCount;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal shippingCost;
        private BigDecimal discountAmount;
        private BigDecimal total;
        private String appliedCoupon;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicableCoupon {
        private String code;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal estimatedSaving;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuickCheckoutResponse {
        private boolean success;
        private String orderNumber;
        private Long transactionId;
        private String message;
        private BigDecimal totalAmount;
    }

    // ── AI Tercihleri ──

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AiPreferenceResponse {
        private Boolean autoApproveEnabled;
        private BigDecimal autoApproveMaxAmount;
        private List<String> autoApproveCategories;
        private Boolean useDefaultAddress;
        private Boolean useDefaultPayment;
        private Integer dailyTransactionLimit;
        private BigDecimal maxOrderAmount;
        private int todayTransactionCount;
    }

    // ── Internal (Python AI ↔ Spring Boot) ──

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternalCartSummary {
        private int itemCount;
        private BigDecimal subtotal;
        private BigDecimal total;
        private String appliedCoupon;
        private boolean hasItems;
        private List<ApplicableCoupon> applicableCoupons;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternalAddressResponse {
        private Long id;
        private String label;
        private String fullName;
        private String phone;
        private String addressLine1;
        private String city;
        private String district;
        private String postalCode;
        private boolean isDefault;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternalPaymentMethod {
        private String type; // CREDIT_CARD, DEBIT_CARD, KAPIDA_ODEME
        private String maskedNumber; // **** **** **** 1234
        private boolean isDefault;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternalUserPreferences {
        private boolean autoApproveEnabled;
        private BigDecimal autoApproveMaxAmount;
        private List<String> autoApproveCategories;
        private boolean useDefaultAddress;
        private boolean useDefaultPayment;
        private int dailyTransactionLimit;
        private BigDecimal maxOrderAmount;
        private int todayTransactionCount;
        private int hourlyTransactionCount;
    }
}
