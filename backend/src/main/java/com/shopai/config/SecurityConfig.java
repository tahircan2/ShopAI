package com.shopai.config;


import com.shopai.security.CsrfCookieFilter;
import com.shopai.security.JwtAuthFilter;
import com.shopai.security.OriginHeaderFilter;
import com.shopai.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.shopai.security.PersistentCookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OriginHeaderFilter originHeaderFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Security Headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()) // Clickjacking protection (X-Frame-Options: DENY)
                        .contentTypeOptions(contentType -> contentType.disable()) // X-Content-Type-Options: nosniff (by default in Spring Security, but explicitly enforcing)
                        // Content-Security-Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:;")
                        )
                )

                // CSRF: PersistentCookieCsrfTokenRepository kullanılır.
                // Spring'in varsayılan CookieCsrfTokenRepository'si başarılı POST sonrası
                // cookie'yi Max-Age=0 ile siler; bu "1-accept/1-refuse" döngüsüne yol açar.
                // Özel repository token'ı hiçbir zaman silmez.
                .csrf(csrf -> csrf
                    .csrfTokenRepository(new PersistentCookieCsrfTokenRepository())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                            "/api/auth/login",
                            "/api/auth/register",
                            "/api/auth/refresh",
                            "/api/auth/logout",
                            "/api/auth/forgot-password",
                            "/api/auth/reset-password",
                            "/api/auth/verify-email",
                            "/api/auth/resend-verification",
                            "/api/admin/products/**"))

                // Stateless — JWT cookie ile yönetilir
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Endpoint yetkilendirme
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Admin and Seller endpoints (Products)
                        .requestMatchers("/api/admin/products/**").hasAnyRole("ADMIN", "SELLER")
                        // Other Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // AI endpoint — anonim kullanıcı da chatbot kullanabilir
                        .requestMatchers("/api/ai/chat", "/api/ai/chat/stream").permitAll()

                        // Geri kalanlar: login zorunlu
                        .anyRequest().authenticated())

                // RateLimitingFilter — Tüm trafiğin en başında, gereksiz DB yükü ve CPU tüketimini engeller
                .addFilterBefore(rateLimitingFilter, CsrfFilter.class)

                // OriginHeaderFilter — CSRF'ten önce çalışarak ek güvenlik sağlar
                .addFilterBefore(originHeaderFilter, CsrfFilter.class)

                // JWT filter'ı UsernamePasswordAuthenticationFilter'dan önce ekle
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // CsrfCookieFilter — her response'ta XSRF-TOKEN cookie'sini yazar
                // (Spring Security 6 lazy token üretir; bu filter tetiklemeyi zorlar)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)


                // Exception handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // cookie göndermek için zorunlu
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
