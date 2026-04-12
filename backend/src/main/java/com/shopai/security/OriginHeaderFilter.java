package com.shopai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mutasyon istekleri (POST, PUT, DELETE, PATCH) için ek koruma katmanı.
 *
 * Sadece CORS ve CSRF token'ı ile yetinmeyip, isteğin "Origin" ve "Referer"
 * header'larını kontrol ederek uygulamanın frontend URL'i dışındaki kaynaklardan
 * gelen sahte istekleri reddeder.
 */
@Slf4j
@Component
public class OriginHeaderFilter extends OncePerRequestFilter {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String allowedOrigin;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();

        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");

            // Eğer Origin yoksa ve Referer da yoksa bazen API client'lar (Postman vb.) olabilir,
            // ama tarayıcılar her mutasyon isteğine bunları ekler (Same-Origin olsa bile Origin yoksa bile
            // en kötü Referer vardır). Tarayıcı kaynaklı saldırılara karşı sıkı Origin doğrulaması:
            boolean isValid = false;

            if (origin != null) {
                isValid = origin.equalsIgnoreCase(allowedOrigin);
            } else if (referer != null) {
                isValid = referer.startsWith(allowedOrigin);
            } else {
                // Güvenli tarafta kalmak adına, tarayıcı harici mutasyonları backend
                // engellemeyebilir, fakat üretim (production) ortamındaysak genellikle
                // sadece frontend beklenir. Strict moda almak için isValid = false bırakıyoruz.
                // Mobile app veya dış entegrasyonlar varsa bir API-KEY mekanizması olmalı burada.
                isValid = true; // Şimdilik katı engeli Postman vs gibi client'ları bozmamak için relaxed bıraktık
                                // (Burası false yapılırsa tam güvenli olur ama non-browser test tool'ları POST yapamaz)
                // Daha iyi bir yöntem CSRF koruması zaten olduğu için bu adımı sadece ekstra katman olarak kullanmaktır.
            }

            if (!isValid && (origin != null || referer != null)) {
                log.warn("Blocked request with cross-site Origin/Referer. Origin: {}, Referer: {}", origin, referer);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Cross-site request blocked by Origin verification\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
