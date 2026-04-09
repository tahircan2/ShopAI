package com.shopai.dto.response;

import com.shopai.entity.Address;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private Long id;
    private String label;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String postalCode;
    private String country;
    private Boolean isDefault;

    public static AddressResponse from(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .district(a.getDistrict())
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .isDefault(a.getIsDefault())
                .build();
    }
}
