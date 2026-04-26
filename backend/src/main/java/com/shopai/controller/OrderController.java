package com.shopai.controller;

import com.shopai.dto.request.OrderRequests.*;
import com.shopai.dto.response.OrderResponses.*;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Sipariş oluşturma, sorgulama ve yönetim")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Yeni sipariş oluştur")
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal JwtAuthDetails auth,
                                                   @Valid @RequestBody CreateOrderRequest req,
                                                   HttpServletRequest request) {
        return ResponseEntity.ok(orderService.createOrder(auth.getUserId(), req, request));
    }

    @GetMapping
    @Operation(summary = "Kullanıcının geçmiş siparişlerini listele")
    public ResponseEntity<Page<OrderSummaryResponse>> getUserOrders(@AuthenticationPrincipal JwtAuthDetails auth,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getUserId(), page, size));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Sipariş detayını getir")
    public ResponseEntity<OrderResponse> getOrderDetail(@AuthenticationPrincipal JwtAuthDetails auth,
                                                      @PathVariable String orderNumber,
                                                      @RequestParam(defaultValue = "false") boolean isAdmin) {
        String role = auth.getRole() != null ? auth.getRole().replace("ROLE_", "") : "";
        boolean isRequestingAdmin = isAdmin && "ADMIN".equals(role);
        boolean isSeller = "SELLER".equals(role);
        return ResponseEntity.ok(orderService.getOrderDetail(auth.getUserId(), orderNumber, isRequestingAdmin, isSeller));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Siparişi iptal et")
    public ResponseEntity<OrderResponse> cancelOrder(@AuthenticationPrincipal JwtAuthDetails auth,
                                                   @PathVariable String orderNumber,
                                                   HttpServletRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(auth.getUserId(), orderNumber, request));
    }

    // ─── Admin Endpoints ───────────────────────────────────────────────────
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tüm siparişleri listele (Admin)")
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sipariş durumunu güncelle (Admin)")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateOrderStatusRequest req,
                                                         HttpServletRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, req, request));
    }
}
