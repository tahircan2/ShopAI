package com.shopai.security;

import com.shopai.service.BlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BlacklistService blacklistService;

    @Value("${app.ai-service.internal-key}")
    private String internalKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String reqInternalKey = request.getHeader("X-Internal-Key");
        if (reqInternalKey != null && reqInternalKey.equals(internalKey)) {
            String userIdStr = request.getHeader("X-Authenticated-User-Id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    
                    // Internal AI servisi okuma işlemleri(sipariş sorgulama vs) için varsayılan USER rolü ver, ama başlıkta geldiyse onu kullan.
                    String userRole = request.getHeader("X-Authenticated-User-Role");
                    if (userRole == null || userRole.isEmpty()) {
                        userRole = "USER";
                    }
                    
                    // Role_ prefixini temizle
                    userRole = userRole.replace("ROLE_", "");
                    
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + userRole));
                    var authDetails = new JwtAuthDetails(userId, "internal@shopai.com", userRole);
                    var authentication = new UsernamePasswordAuthenticationToken(authDetails, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                    return;
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-Authenticated-User-Id format: {}", userIdStr);
                }
            }
        }

        String token = extractTokenFromCookie(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Kara liste kontrolü — logout yapılmış token'ları reddet
        if (blacklistService.isBlacklisted(token)) {
            log.debug("Rejecting blacklisted token for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                String email = jwtUtil.extractEmail(token);

                // SecurityContext'e Authentication set et — role JWT imzasından gelir,
                // client'tan DEĞİL
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authDetails = new JwtAuthDetails(userId, email, role);
                var authentication = new UsernamePasswordAuthenticationToken(authDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            log.debug("Access token expired for request: {}", request.getRequestURI());
            // Token süresi dolmuşsa 401 + expired flag — Angular interceptor bunu yakalar
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"expired\":true}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token'ı YALNIZCA HttpOnly cookie'den okur.
     * Authorization header'dan asla okumaz — güvenlik gereği.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Public endpoint'lerde filter atla (performans için)
        // Logout'u da ekliyoruz ki token expired
        // olsa bile cookie'leri temizleyebilelim.
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/auth/verify-email")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs");
    }
}
