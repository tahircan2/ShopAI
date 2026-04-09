package com.shopai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class UserRequests {

    @Data
    public static class UpdateProfileRequest {
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
    public static class AddressRequest {
        @Size(max = 50)
        private String label;

        @NotBlank(message = "Ad Soyad zorunludur")
        @Size(max = 150)
        private String fullName;

        @Size(max = 20)
        private String phone;

        @NotBlank(message = "Adres zorunludur")
        @Size(max = 255)
        private String addressLine1;

        @Size(max = 255)
        private String addressLine2;

        @NotBlank(message = "Şehir zorunludur")
        @Size(max = 100)
        private String city;

        @Size(max = 100)
        private String district;

        @Size(max = 10)
        private String postalCode;

        @Size(max = 50)
        private String country;

        private Boolean isDefault;
    }
}
