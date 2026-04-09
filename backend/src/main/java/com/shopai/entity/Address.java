package com.shopai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address",
        indexes = {
            @Index(name = "idx_address_user", columnList = "user_id"),
            @Index(name = "idx_address_default", columnList = "user_id, is_default")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String label;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(length = 50)
    @Builder.Default
    private String country = "Türkiye";

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
