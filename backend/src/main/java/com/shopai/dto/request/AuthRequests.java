package com.shopai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequests {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email zorunludur")
        @Email(message = "Geçerli bir email adresi giriniz")
        private String email;

        @NotBlank(message = "Şifre zorunludur")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Email zorunludur")
        @Email(message = "Geçerli bir email adresi giriniz")
        private String email;

        @NotBlank(message = "Şifre zorunludur")
        @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
        private String password;

        @NotBlank(message = "Ad zorunludur")
        @Size(max = 100)
        private String firstName;

        @NotBlank(message = "Soyad zorunludur")
        @Size(max = 100)
        private String lastName;

        @Size(max = 20)
        private String phone;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank
        @Email
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token zorunludur")
        private String token;

        @NotBlank(message = "Yeni şifre zorunludur")
        @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
        private String newPassword;
    }

    @Data
    public static class ResendVerificationRequest {
        @NotBlank
        @Email
        private String email;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Mevcut şifre zorunludur")
        private String currentPassword;

        @NotBlank(message = "Yeni şifre zorunludur")
        @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
        private String newPassword;
    }
}
