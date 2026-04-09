package com.shopai.scheduler;

import com.shopai.repository.RefreshTokenRepository;
import com.shopai.repository.UserRepository;
import com.shopai.repository.UserSessionRepository;
import com.shopai.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Her gece 02:00'de süresi dolmuş ve iptal edilmiş refresh token'ları,
     * ve süresi dolmuş kullanıcı oturumlarını temizler.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedRefreshTokens = refreshTokenRepository.deleteExpiredAndRevoked(now);
        int deactivatedSessions = userSessionRepository.deactivateExpiredSessions(now);

        log.info("Token cleanup completed: {} expired/revoked tokens deleted, {} sessions deactivated", 
                deletedRefreshTokens, deactivatedSessions);

        if (deletedRefreshTokens > 0 || deactivatedSessions > 0) {
            auditLogService.logWithMap(null, AuditLogService.Actions.TOKEN_CLEANUP, "Scheduler", null, null,
                    Map.of("deletedRefreshTokens", deletedRefreshTokens, "deactivatedSessions", deactivatedSessions),
                    null, null);
        }
    }

    /**
     * Her 15 dakikada bir çalışarak hesap kilitlerini kontrol eder.
     * kilit süresi dolmuş hesapları tekrar giriş yapabilmeleri için kilitlerini açar.
     */
    @Scheduled(fixedRate = 900_000) // 15 dakika
    @Transactional
    public void unlockExpiredAccounts() {
        int unlockedAccounts = userRepository.unlockExpiredAccounts(LocalDateTime.now());
        if (unlockedAccounts > 0) {
            log.info("Account unlock completed: {} accounts unlocked automatically.", unlockedAccounts);
        } else {
            log.debug("Account unlock checked. No accounts needed unlocking.");
        }
    }
}
