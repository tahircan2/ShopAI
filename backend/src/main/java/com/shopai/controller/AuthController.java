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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Kimlik doğrulama, kayıt ve şifre yönetimi")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Yeni kullanıcı kaydı")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Kullanıcı girişi (HttpOnly cookie set eder)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, 
                                            HttpServletRequest request, 
                                            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(req, request, response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Token yenileme")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, 
                                              HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Çıkış yap (Cookie'leri temizler)")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal JwtAuthDetails auth,
                                     HttpServletRequest request, 
                                     HttpServletResponse response) {
        authService.logout(auth != null ? auth.getUserId() : null, request, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Mevcut oturum durumunu getir")
    public ResponseEntity<SessionStatus> me(@AuthenticationPrincipal JwtAuthDetails auth) {
        if (auth == null) {
            return ResponseEntity.ok(SessionStatus.builder().isLoggedIn(false).build());
        }
        return ResponseEntity.ok(authService.checkSession(auth.getUserId()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifremi unuttum maili gönder")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifre sıfırlama işlemini tamamla")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "E-posta doğrulama")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Doğrulama e-postasını tekrar gönder")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        return ResponseEntity.ok(authService.resendVerification(req));
    }
}
