package com.shopai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6, CSRF token'ı lazy (tembel) üretir.
 * Bu filter her yanıtta CsrfToken.getToken() çağırarak
 * CookieCsrfTokenRepository'nin XSRF-TOKEN cookie'sini
 * response'a yazmasını zorlar.
 *
 * NOT: XorCsrfTokenRequestAttributeHandler ile kullanılmaz.
 * Sadece CsrfTokenRequestAttributeHandler ile kullanılır;
 * böylece token değeri istekler arasında sabit kalır ve
 * Angular'ın 1-accept/1-refuse sorunu yaşanmaz.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Token'a erişmek, CookieCsrfTokenRepository'nin
            // XSRF-TOKEN cookie'sini response'a yazmasını tetikler.
            // CsrfTokenRequestAttributeHandler ile token değeri değişmez,
            // bu yüzden her istekte çevrimde güvenle çağrılabilir.
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
