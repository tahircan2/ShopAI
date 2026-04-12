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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Sipariş işlemleri")
public class OrderController {

    private final OrderService orderService;

    // ─── Kullanıcı Endpoint'leri ─────────────────────────────────────────────

    @PostMapping("/api/orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Siparişi tamamla — sepetten sipariş oluştur")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody CreateOrderRequest req,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(auth.getUserId(), req, request));
    }

    @GetMapping("/api/users/me/orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sipariş geçmişi — sadece JWT sahibine ait")
    public ResponseEntity<Page<OrderSummaryResponse>> getUserOrders(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getUserId(), page, size));
    }

    @GetMapping("/api/orders/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sipariş detayı — ownership check zorunlu")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable String orderNumber) {
        // Kullanıcı yalnızca kendi siparişini görebilir — ownership check OrderService'te
        return ResponseEntity.ok(orderService.getOrderDetail(auth.getUserId(), orderNumber));
    }

    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable String orderNumber,
            HttpServletRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(auth.getUserId(), orderNumber, request));
    }

    // ─── Admin Endpoint'leri ─────────────────────────────────────────────────

    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tüm siparişler (Admin)")
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @PutMapping("/api/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sipariş durumu güncelle (Admin)")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, req, request));
    }
}
