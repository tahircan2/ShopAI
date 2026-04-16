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
     * Logout — her iki cookie'yi de sıfır MaxAge ile hem domain'li hem domain'siz
     * ve hem ana path hem de özel path'ler için temizle (Nuclear Clear).
     */
    public void clearAuthCookies(HttpServletResponse response) {
        String[] paths = {"/api", "/api/auth/refresh", "/"};
        String[] domains = {null, cookieDomain};

        for (String path : paths) {
            for (String domain : domains) {
                if (domain != null && domain.isBlank()) continue;

                // access_token temizle
                response.addHeader("Set-Cookie", createDeleteCookie("access_token", path, domain));
                // refresh_token temizle
                response.addHeader("Set-Cookie", createDeleteCookie("refresh_token", path, domain));
                // XSRF-TOKEN temizle ( Angular/Csrf için)
                response.addHeader("Set-Cookie", createDeleteCookie("XSRF-TOKEN", path, domain));
            }
        }
    }

    private String createDeleteCookie(String name, String path, String domain) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(!name.equals("XSRF-TOKEN")) // XSRF-TOKEN JS tarafından okunabilmeli
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0);
        
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        
        return builder.build().toString();
    }

    /**
     * Request'ten belirtilen isimli cookie'yi okur
     */
    public String extractTokenFromCookie(jakarta.servlet.http.HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Request'ten refresh token cookie'sini okur
     */
    public String extractRefreshTokenFromCookie(jakarta.servlet.http.HttpServletRequest request) {
        return extractTokenFromCookie(request, "refresh_token");
    }
}
