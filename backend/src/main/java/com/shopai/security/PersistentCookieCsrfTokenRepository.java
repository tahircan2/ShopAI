package com.shopai.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.UUID;

/**
 * Özel CSRF Token Repository.
 *
 * Spring Security'nin varsayılan CookieCsrfTokenRepository'si,
 * başarılı bir POST isteğini doğruladıktan sonra cookie'yi
 * (Max-Age=0 ile) SİLER. Bu, Angular SPA'larda "1 accept / 1 refuse"
 * döngüsüne neden olur.
 *
 * Bu repository ise token'ı HİÇBİR ZAMAN silmez. Token bir kez
 * oluşturulur, cookie ömrü boyunca geçerli kalır. Sunucu tarafı
 * token doğrulama Spring Security standart akışıyla yapılır.
 */
public class PersistentCookieCsrfTokenRepository implements CsrfTokenRepository {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAM_NAME = "_csrf";
    private static final int COOKIE_MAX_AGE = 3600 * 8; // 8 saat

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // token == null ise Spring Security "sil" diyor — BİZ SILMIYORUZ.
        // Mevcut cookie değerini request'ten okuyup aynısını geri yazıyoruz.
        if (token == null) {
            // Mevcut token'ı koru: cookie'yi güncelleme
            return;
        }

        Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token.getToken());
        cookie.setHttpOnly(false);   // Angular JS'den okuyabilmeli
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        // cookie.setSecure(true); // HTTPS'de aktif et
        response.addCookie(cookie);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie c : request.getCookies()) {
            if (CSRF_COOKIE_NAME.equals(c.getName())) {
                String value = c.getValue();
                if (value == null || value.isBlank()) return null;
                return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, value);
            }
        }
        return null;
    }
}
