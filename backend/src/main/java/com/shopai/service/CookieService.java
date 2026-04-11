package com.shopai.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CookieService {

    @Value("${app.cookie.secure}")
    private boolean secure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain; // Boş string: tarayıcı mevcut host'u kullanır

    @Value("${app.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Domain ayarı yoksa (boş string) ResponseCookie'ye domain set etmez.
     * Bu durumda tarayıcı mevcut request host'unu kullanır (en güvenli davranış).
     */
    private ResponseCookie.ResponseCookieBuilder applyDomain(ResponseCookie.ResponseCookieBuilder builder) {
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder;
    }

    /**
     * Access token cookie — HttpOnly; Secure; SameSite=Strict; Path=/api
     * JS ile document.cookie üzerinden erişilemez — XSS koruması
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = applyDomain(
                ResponseCookie.from("access_token", token)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite(sameSite)
                        .path("/api")
                        .maxAge(accessExpiration / 1000))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Refresh token cookie — HttpOnly; Secure; SameSite=Strict
     * Path=/api/auth/refresh ile yalnızca ilgili endpoint'e gider
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = applyDomain(
                ResponseCookie.from("refresh_token", token)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite(sameSite)
                        .path("/api/auth/refresh")
                        .maxAge(refreshExpiration / 1000))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Logout — her iki cookie'yi de sıfır MaxAge ile sil
     */
    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    /**
     * Request'ten refresh token cookie'sini okur
     */
    public String extractRefreshTokenFromCookie(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
