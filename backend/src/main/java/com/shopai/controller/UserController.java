package com.shopai.controller;

import com.shopai.dto.request.AuthRequests.ChangePasswordRequest;
import com.shopai.dto.request.UserRequests.*;
import com.shopai.dto.response.AddressResponse;
import com.shopai.dto.response.AuthResponses.MessageResponse;
import com.shopai.dto.response.AuthResponses.UserInfo;
import com.shopai.dto.response.ProductResponses.ProductSummaryResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "User", description = "Kullanıcı profil ve adres işlemleri")
public class UserController {

    private final UserService userService;

    // ─── Profil ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Mevcut kullanıcı profili — userId JWT'den alınır")
    public ResponseEntity<UserInfo> getProfile(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getUserId()));
    }

    @PutMapping
    @Operation(summary = "Profil güncelle")
    public ResponseEntity<UserInfo> updateProfile(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody UpdateProfileRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateProfile(auth.getUserId(), req, request));
    }

    @PutMapping("/password")
    @Operation(summary = "Şifre değiştir")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody ChangePasswordRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        userService.changePassword(auth.getUserId(), req, request);
        return ResponseEntity.ok(new MessageResponse("Şifreniz başarıyla değiştirildi."));
    }

    // ─── Adresler ────────────────────────────────────────────────────────────

    @GetMapping("/addresses")
    @Operation(summary = "Adres listesi — sadece JWT sahibine ait")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getAddresses(auth.getUserId()));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Yeni adres ekle")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody AddressRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addAddress(auth.getUserId(), req, request));
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Adres güncelle — ownership check yapılır")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateAddress(auth.getUserId(), addressId, req, request));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Adres sil — ownership check yapılır")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long addressId,
            jakarta.servlet.http.HttpServletRequest request) {
        userService.deleteAddress(auth.getUserId(), addressId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Wishlist ────────────────────────────────────────────────────────────

    @GetMapping("/wishlist")
    @Operation(summary = "Favori listesi")
    public ResponseEntity<List<ProductSummaryResponse>> getWishlist(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(userService.getWishlist(auth.getUserId()));
    }

    @PostMapping("/wishlist/{productId}")
    @Operation(summary = "Favoriye ekle")
    public ResponseEntity<Void> addToWishlist(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long productId) {
        userService.addToWishlist(auth.getUserId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/wishlist/{productId}")
    @Operation(summary = "Favoriden çıkar")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long productId) {
        userService.removeFromWishlist(auth.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    // ─── Yorumlarım ─────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    @Operation(summary = "Kullanıcının yaptığı yorumlar")
    public ResponseEntity<org.springframework.data.domain.Page<com.shopai.dto.response.ProductResponses.ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getMyReviews(auth.getUserId(), page, size));
    }
}
