package com.shopai.dto.response;

import com.shopai.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthResponses {

    /**
     * Login/Register yanıtı — token string'i body'de dönmez, HttpOnly cookie'de taşınır.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private long expiresIn;   // saniye cinsinden (access token süresi)
        private UserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private Boolean isEmailVerified;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private String shopName;
        private String shopDescription;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .isEmailVerified(user.getIsEmailVerified())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .shopName(user.getShopName())
                    .shopDescription(user.getShopDescription())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStatus {
        private boolean isLoggedIn;
        private UserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}
