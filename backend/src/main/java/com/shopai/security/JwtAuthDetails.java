package com.shopai.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JWT'den extract edilen ve SecurityContext'te tutulan kullanıcı bilgileri.
 * Controller'lar bu nesneyi @AuthenticationPrincipal ile alır.
 */
@Getter
@AllArgsConstructor
public class JwtAuthDetails {
    private final Long userId;
    private final String email;
    private final String role;
}
