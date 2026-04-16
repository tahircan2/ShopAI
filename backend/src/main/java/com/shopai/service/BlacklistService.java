package com.shopai.service;

import com.shopai.entity.BlacklistedToken;
import com.shopai.repository.BlacklistedTokenRepository;
import com.shopai.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final BlacklistedTokenRepository blacklistRepository;
    private final JwtUtil jwtUtil;

    /**
     * Token'ı kara listeye ekler.
     */
    @Transactional
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) return;

        try {
            Claims claims = jwtUtil.parseToken(token);
            LocalDateTime expiryDate = claims.getExpiration()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            if (expiryDate.isBefore(LocalDateTime.now())) {
                log.debug("Token already expired, no need to blacklist");
                return;
            }

            if (!blacklistRepository.existsByToken(token)) {
                BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                        .token(token)
                        .expiryDate(expiryDate)
                        .build();
                blacklistRepository.save(blacklistedToken);
                log.info("Token added to blacklist. Expiry: {}", expiryDate);
            }
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Token'ın kara listede olup olmadığını kontrol eder.
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        return blacklistRepository.existsByToken(token);
    }

    /**
     * Süresi dolmuş kara liste kayıtlarını temizler (Her gece 03:00'te).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired blacklisted tokens...");
        blacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleanup completed.");
    }
}
