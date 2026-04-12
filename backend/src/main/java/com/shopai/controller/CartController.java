package com.shopai.controller;

import com.shopai.dto.request.CartRequests.*;
import com.shopai.dto.response.CartResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Sepet işlemleri")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Kullanıcının sepetini getir")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal JwtAuthDetails auth) {
        // userId JWT'den alınır — query param veya body'den asla kabul edilmez
        return ResponseEntity.ok(cartService.getCart(auth.getUserId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Sepete ürün ekle")
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody AddToCartRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(cartService.addToCart(auth.getUserId(), req, request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Ürün miktarını güncelle")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(auth.getUserId(), itemId, req, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Ürünü sepetten çıkar")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long itemId,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(cartService.removeItem(auth.getUserId(), itemId, request));
    }

    @DeleteMapping
    @Operation(summary = "Sepeti temizle")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal JwtAuthDetails auth, jakarta.servlet.http.HttpServletRequest request) {
        cartService.clearCart(auth.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/coupon")
    @Operation(summary = "Kupon uygula")
    public ResponseEntity<CartResponse> applyCoupon(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody ApplyCouponRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(cartService.applyCoupon(auth.getUserId(), req, request));
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Kuponu kaldır")
    public ResponseEntity<CartResponse> removeCoupon(@AuthenticationPrincipal JwtAuthDetails auth, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(cartService.removeCoupon(auth.getUserId(), request));
    }
}
