package com.shopai.controller;

import com.shopai.dto.request.AuthRequests.*;
import com.shopai.dto.response.AuthResponses.*;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Kimlik doğrulama işlemleri")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Yeni kullanıcı kaydı")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Giriş — access + refresh token HttpOnly cookie olarak set edilir")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(req, request, response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token cookie ile yeni access token al")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Çıkış — cookie'leri temizle, token'ları geçersiz kıl")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = authDetails != null ? authDetails.getUserId() : null;
        authService.logout(userId, request, response);
        return ResponseEntity.ok(new MessageResponse("Başarıyla çıkış yapıldı."));
    }

    @GetMapping("/me")
    @Operation(summary = "Mevcut oturum durumu ve kullanıcı bilgisi")
    public ResponseEntity<SessionStatus> me(@AuthenticationPrincipal JwtAuthDetails authDetails) {
        if (authDetails == null) {
            return ResponseEntity.ok(new SessionStatus(false, null));
        }
        return ResponseEntity.ok(authService.checkSession(authDetails.getUserId()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifre sıfırlama e-postası gönder")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Token ile yeni şifre belirle")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "E-posta doğrulama linki")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Doğrulama e-postasını yeniden gönder")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        return ResponseEntity.ok(authService.resendVerification(req));
    }
}
