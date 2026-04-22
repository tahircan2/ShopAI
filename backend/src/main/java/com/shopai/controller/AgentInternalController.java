package com.shopai.controller;

import com.shopai.dto.request.AgentRequests.CreateApprovalRequest;
import com.shopai.dto.request.CartRequests.*;
import com.shopai.dto.response.AgentResponses.*;
import com.shopai.dto.response.CartResponse;
import com.shopai.entity.AgentTransaction;
import com.shopai.entity.AgentTransaction.TransactionType;
import com.shopai.entity.AgentTransactionStep;
import com.shopai.entity.User;
import com.shopai.exception.ForbiddenException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.AddressRepository;
import com.shopai.repository.UserRepository;
import com.shopai.service.AgentApprovalService;
import com.shopai.service.AgentTransactionService;
import com.shopai.service.UserAiPreferenceService;
import com.shopai.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Servisinin Dahili Kullanımı İçin Controller.
 * Sadece Python tarafı (dahili anahtar ile) bu uçlara erişebilir.
 */
@RestController
@RequestMapping("/api/internal/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Internal", description = "Python AI servisi ile backend arasındaki dahili iletişim")
public class AgentInternalController {

    private final AgentTransactionService transactionService;
    private final AgentApprovalService approvalService;
    private final UserAiPreferenceService preferenceService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Value("${app.ai-service.internal-key}")
    private String expectedInternalKey;

    // ── Transaction Endpoints ──

    @PostMapping("/transactions/start")
    @Operation(summary = "Yeni bir çok adımlı işlem başlat")
    public ResponseEntity<TransactionResponse> startTransaction(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestParam String type,
            @RequestParam int totalSteps) {

        validateKey(internalKey);
        AgentTransaction transaction = transactionService.startTransaction(userId, sessionId, TransactionType.valueOf(type), totalSteps);
        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }

    @PostMapping("/transactions/{id}/steps/add")
    public ResponseEntity<Void> addStep(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long id,
            @RequestParam int order,
            @RequestParam String type,
            @RequestParam String description,
            @RequestBody Map<String, Object> requestData) {

        validateKey(internalKey);
        transactionService.addStep(id, order, AgentTransactionStep.StepType.valueOf(type), description, requestData);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transactions/{id}/steps/complete")
    public ResponseEntity<Void> completeStep(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long id,
            @RequestParam int order,
            @RequestParam long duration,
            @RequestBody Map<String, Object> responseData) {

        validateKey(internalKey);
        transactionService.completeStep(id, order, responseData, duration);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transactions/{id}/fail")
    public ResponseEntity<Void> failTransaction(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long id,
            @RequestParam String errorMessage) {

        validateKey(internalKey);
        transactionService.failTransaction(id, errorMessage);
        return ResponseEntity.ok().build();
    }

    // ── Approval Endpoints (Internal — Python AI Servisi İçin) ──

    @PostMapping("/approvals/create")
    @Operation(summary = "Python AI servisi için onay kaydı oluştur")
    public ResponseEntity<ApprovalResponse> createApprovalInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @Valid @RequestBody CreateApprovalRequest req) {

        validateKey(internalKey);
        User user = findUser(userId);
        return ResponseEntity.ok(approvalService.createApproval(user, req));
    }

    @GetMapping("/approvals/latest")
    @Operation(summary = "Kullanıcının son aktif (PENDING/APPROVED) onay kaydını getir")
    public ResponseEntity<ApprovalResponse> getLatestApprovalInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId) {

        validateKey(internalKey);
        User user = findUser(userId);

        return approvalService.getLatestActiveApproval(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/approvals/{token}/approve")
    public ResponseEntity<ApprovalActionResponse> approveInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @PathVariable String token) {

        validateKey(internalKey);
        User user = findUser(userId);
        return ResponseEntity.ok(approvalService.approve(user, token));
    }

    @PostMapping("/approvals/{token}/reject")
    public ResponseEntity<ApprovalActionResponse> rejectInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @PathVariable String token) {

        validateKey(internalKey);
        User user = findUser(userId);
        return ResponseEntity.ok(approvalService.reject(user, token));
    }

    // ── User Data Endpoints (Checkout Agent İçin) ──

    @GetMapping("/user/{userId}/default-address")
    @Operation(summary = "Kullanıcının varsayılan adresini getir")
    public ResponseEntity<?> getDefaultAddress(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long userId) {

        validateKey(internalKey);
        return addressRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .map(addr -> ResponseEntity.ok(InternalAddressResponse.builder()
                        .id(addr.getId())
                        .label(addr.getLabel())
                        .fullName(addr.getFullName())
                        .addressLine1(addr.getAddressLine1())
                        .city(addr.getCity())
                        .isDefault(addr.getIsDefault())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/preferences")
    @Operation(summary = "Kullanıcının AI tercihlerini getir")
    public ResponseEntity<InternalUserPreferences> getUserPreferences(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long userId) {

        validateKey(internalKey);
        try {
            var pref = preferenceService.getPreferences(userId);
            return ResponseEntity.ok(InternalUserPreferences.builder()
                    .autoApproveEnabled(pref.getAutoApproveEnabled())
                    .autoApproveMaxAmount(pref.getAutoApproveMaxAmount())
                    .useDefaultAddress(pref.getUseDefaultAddress())
                    .useDefaultPayment(pref.getUseDefaultPayment())
                    .dailyTransactionLimit(pref.getDailyTransactionLimit())
                    .maxOrderAmount(pref.getMaxOrderAmount())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/cart-summary")
    @Operation(summary = "Kullanıcının sepet özetini getir")
    public ResponseEntity<?> getCartSummary(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable Long userId) {

        validateKey(internalKey);
        try {
            var cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Cart Modification Endpoints (Internal) ──

    @PostMapping("/cart/items")
    @Operation(summary = "Dahili: Sepete ürün ekle")
    public ResponseEntity<CartResponse> addToCartInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @Valid @RequestBody AddToCartRequest req,
            HttpServletRequest request) {

        validateKey(internalKey);
        return ResponseEntity.ok(cartService.addToCart(userId, req, request));
    }

    @PutMapping("/cart/items/{itemId}")
    @Operation(summary = "Dahili: Sepet miktarını güncelle")
    public ResponseEntity<CartResponse> updateCartQuantityInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest req,
            HttpServletRequest request) {

        validateKey(internalKey);
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, req, request));
    }

    @DeleteMapping("/cart/items/{itemId}")
    @Operation(summary = "Dahili: Ürünü sepetten çıkar")
    public ResponseEntity<CartResponse> removeCartItemInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            @PathVariable Long itemId,
            HttpServletRequest request) {

        validateKey(internalKey);
        return ResponseEntity.ok(cartService.removeItem(userId, itemId, request));
    }

    @DeleteMapping("/cart")
    @Operation(summary = "Dahili: Sepeti temizle")
    public ResponseEntity<Void> clearCartInternal(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestHeader("X-Authenticated-User-Id") Long userId,
            HttpServletRequest request) {

        validateKey(internalKey);
        cartService.clearCart(userId, request);
        return ResponseEntity.ok().build();
    }

    // ── Security ──

    private void validateKey(String actualKey) {
        if (actualKey == null || !actualKey.equals(expectedInternalKey)) {
            throw new ForbiddenException("Geçersiz dahili anahtar");
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }
}
