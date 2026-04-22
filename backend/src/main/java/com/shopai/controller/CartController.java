package com.shopai.controller;

import com.shopai.dto.request.CartRequests.*;
import com.shopai.dto.response.CartResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Sepet yönetimi ve kupon işlemleri")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Mevcut sepeti getir")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(cartService.getCart(auth.getUserId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Sepete ürün ekle")
    public ResponseEntity<CartResponse> addToCart(@AuthenticationPrincipal JwtAuthDetails auth,
                                                @Valid @RequestBody AddToCartRequest req,
                                                HttpServletRequest request) {
        return ResponseEntity.ok(cartService.addToCart(auth.getUserId(), req, request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Sepetteki ürün miktarını güncelle")
    public ResponseEntity<CartResponse> updateQuantity(@AuthenticationPrincipal JwtAuthDetails auth,
                                                     @PathVariable Long itemId,
                                                     @Valid @RequestBody UpdateQuantityRequest req,
                                                     HttpServletRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(auth.getUserId(), itemId, req, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Ürünü sepetten çıkar")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal JwtAuthDetails auth,
                                                 @PathVariable Long itemId,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(cartService.removeItem(auth.getUserId(), itemId, request));
    }

    @DeleteMapping
    @Operation(summary = "Sepeti tamamen temizle")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal JwtAuthDetails auth,
                                        HttpServletRequest request) {
        cartService.clearCart(auth.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/coupon")
    @Operation(summary = "Sepete kupon kodu uygula")
    public ResponseEntity<CartResponse> applyCoupon(@AuthenticationPrincipal JwtAuthDetails auth,
                                                  @Valid @RequestBody ApplyCouponRequest req,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok(cartService.applyCoupon(auth.getUserId(), req, request));
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Sepetteki kuponu kaldır")
    public ResponseEntity<CartResponse> removeCoupon(@AuthenticationPrincipal JwtAuthDetails auth,
                                                   HttpServletRequest request) {
        return ResponseEntity.ok(cartService.removeCoupon(auth.getUserId(), request));
    }
}
