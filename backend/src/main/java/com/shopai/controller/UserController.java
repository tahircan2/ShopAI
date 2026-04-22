package com.shopai.controller;

import com.shopai.dto.request.AuthRequests.ChangePasswordRequest;
import com.shopai.dto.request.UserRequests.*;
import com.shopai.dto.response.AddressResponse;
import com.shopai.dto.response.AuthResponses.MessageResponse;
import com.shopai.dto.response.AuthResponses.UserInfo;
import com.shopai.dto.response.OrderResponses.OrderSummaryResponse;
import com.shopai.dto.response.ProductResponses.ProductSummaryResponse;
import com.shopai.dto.response.ProductResponses.ReviewResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.OrderService;
import com.shopai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Kullanıcı profil, adres ve favori yönetimi")
public class UserController {

    private final UserService userService;
    private final OrderService orderService;
    private final com.shopai.service.NotificationService notificationService;

    // ─── Profil ──────────────────────────────────────────────────────────────
    @GetMapping({"/me", "/me/profile"})
    @Operation(summary = "Kullanıcı profil bilgilerini getir")
    public ResponseEntity<UserInfo> getProfile(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getUserId()));
    }

    @PutMapping({"/me", "/me/profile"})
    @Operation(summary = "Profil bilgilerini güncelle")
    public ResponseEntity<UserInfo> updateProfile(@AuthenticationPrincipal JwtAuthDetails auth,
                                                @Valid @RequestBody UpdateProfileRequest req,
                                                HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateProfile(auth.getUserId(), req, request));
    }

    @GetMapping("/me/notifications")
    @Operation(summary = "Bildirimleri listele")
    public ResponseEntity<List<com.shopai.service.NotificationService.NotificationResponse>> getNotifications(@AuthenticationPrincipal JwtAuthDetails auth) {
        // Frontend expects List<Notification>, NotificationController returns Page.
        // We'll return the first 50 notifications for simplicity
        return ResponseEntity.ok(notificationService.getNotifications(auth.getUserId(), 0, 50).getContent());
    }

    @PutMapping("/me/notifications/{id}/read")
    @Operation(summary = "Bildirimi okundu olarak işaretle")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal JwtAuthDetails auth,
                                         @PathVariable Long id) {
        notificationService.markAsRead(auth.getUserId(), id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Şifre değiştir")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal JwtAuthDetails auth,
                                                        @Valid @RequestBody ChangePasswordRequest req,
                                                        HttpServletRequest request) {
        userService.changePassword(auth.getUserId(), req, request);
        return ResponseEntity.ok(new MessageResponse("Şifreniz başarıyla değiştirildi."));
    }

    // ─── Adresler ────────────────────────────────────────────────────────────
    @GetMapping("/me/addresses")
    @Operation(summary = "Kayıtlı adresleri listele")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getAddresses(auth.getUserId()));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Yeni adres ekle")
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal JwtAuthDetails auth,
                                                    @Valid @RequestBody AddressRequest req,
                                                    HttpServletRequest request) {
        return ResponseEntity.ok(userService.addAddress(auth.getUserId(), req, request));
    }

    @PutMapping("/me/addresses/{id}")
    @Operation(summary = "Adres güncelle")
    public ResponseEntity<AddressResponse> updateAddress(@AuthenticationPrincipal JwtAuthDetails auth,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody AddressRequest req,
                                                       HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateAddress(auth.getUserId(), id, req, request));
    }

    @DeleteMapping("/me/addresses/{id}")
    @Operation(summary = "Adres sil")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal JwtAuthDetails auth,
                                            @PathVariable Long id,
                                            HttpServletRequest request) {
        userService.deleteAddress(auth.getUserId(), id, request);
        return ResponseEntity.ok().build();
    }

    // ─── Wishlist (Favoriler) ────────────────────────────────────────────────
    @GetMapping("/me/wishlist")
    @Operation(summary = "Favori ürünleri listele")
    public ResponseEntity<List<ProductSummaryResponse>> getWishlist(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getWishlist(auth.getUserId()));
    }

    @PostMapping("/me/wishlist/{productId}")
    @Operation(summary = "Ürünü favorilere ekle")
    public ResponseEntity<Void> addToWishlist(@AuthenticationPrincipal JwtAuthDetails auth,
                                            @PathVariable Long productId) {
        userService.addToWishlist(auth.getUserId(), productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/wishlist/{productId}")
    @Operation(summary = "Ürünü favorilerden çıkar")
    public ResponseEntity<Void> removeFromWishlist(@AuthenticationPrincipal JwtAuthDetails auth,
                                                 @PathVariable Long productId) {
        userService.removeFromWishlist(auth.getUserId(), productId);
        return ResponseEntity.ok().build();
    }

    // ─── Yorumlarım ─────────────────────────────────────────────────────────
    @GetMapping("/me/reviews")
    @Operation(summary = "Kullanıcının yaptığı yorumları listele")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(@AuthenticationPrincipal JwtAuthDetails auth,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getMyReviews(auth.getUserId(), page, size));
    }

    // ─── Siparişlerim ────────────────────────────────────────────────────────
    @GetMapping("/me/orders")
    @Operation(summary = "Kullanıcının siparişlerini listele")
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(@AuthenticationPrincipal JwtAuthDetails auth,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getUserId(), page, size));
    }
}
