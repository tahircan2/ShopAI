package com.shopai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.entity.PendingApproval;
import com.shopai.entity.User;
import com.shopai.entity.UserAiPreference;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.AgentTransactionRepository;
import com.shopai.repository.PendingApprovalRepository;
import com.shopai.repository.UserAiPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentSecurityService {

    private final PendingApprovalRepository pendingApprovalRepository;
    private final AgentTransactionRepository agentTransactionRepository;
    private final UserAiPreferenceRepository userAiPreferenceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Plan datası için SHA-256 hash hesaplar.
     */
    public String calculatePlanHash(String planData) {
        try {
            // JSON'ı normalize et (sıralı key'ler, boşluksuz vb.)
            String normalizedData = canonicalizeJson(planData);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Güvenlik algoritması hatası", e);
        }
    }

    private String canonicalizeJson(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            // ObjectMapper'ı yapılandır (Sıralı key'ler için)
            objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON normalizasyonu yapılamadı, orijinal hali kullanılacak: {}", e.getMessage());
            return json;
        }
    }

    /**
     * Yeni bir onay token'ı oluşturur ve PendingApproval kaydını kaydeder.
     */
    @Transactional
    public PendingApproval createApprovalToken(User user, String planData, String agentType, String sessionId) {
        String token = UUID.randomUUID().toString();
        String hash = calculatePlanHash(planData);

        PendingApproval approval = PendingApproval.builder()
                .user(user)
                .approvalToken(token)
                .planData(planData)
                .planHash(hash)
                .agentType(agentType)
                .expiresAt(LocalDateTime.now().plusSeconds(600))
                .status(PendingApproval.ApprovalStatus.PENDING)
                .build();

        return pendingApprovalRepository.save(approval);
    }

    /**
     * Token'ın geçerliliğini ve plan bütünlüğünü doğrular.
     */
    @Transactional
    public boolean verifyPlanIntegrity(String token, String currentPlanData) {
        PendingApproval approval = pendingApprovalRepository.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz veya süresi dolmuş onay token'ı"));

        if (!approval.canBeUsed()) {
            throw new IllegalStateException("Bu onay token'ı artık kullanılamaz (Süresi dolmuş veya zaten kullanılmış)");
        }

        String currentHash = calculatePlanHash(currentPlanData);
        if (!approval.getPlanHash().equals(currentHash)) {
            log.warn("PLAN_TAMPERED: Plan hash mismatch!");
            log.warn("  Token: {}", token);
            log.warn("  Expected Hash: {}", approval.getPlanHash());
            log.warn("  Current Hash:  {}", currentHash);
            log.warn("  Current Data:  {}", currentPlanData);
            return false;
        }

        return true;
    }

    /**
     * Günlük işlem limitini kontrol eder.
     */
    @Transactional
    public void checkDailyLimit(Long userId) {
        UserAiPreference prefs = userAiPreferenceRepository.findByUserId(userId)
                .orElse(UserAiPreference.builder().dailyTransactionLimit(10).build());

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayCount = agentTransactionRepository.countByUserIdAndCreatedAtAfter(userId, startOfDay);

        if (todayCount >= prefs.getDailyTransactionLimit()) {
            throw new IllegalStateException("Günlük AI işlem limitinize ulaştınız (" + prefs.getDailyTransactionLimit() + ")");
        }
    }

    /**
     * Sipariş tutar limitini kontrol eder.
     */
    @Transactional
    public void checkAmountLimit(Long userId, BigDecimal amount) {
        UserAiPreference prefs = userAiPreferenceRepository.findByUserId(userId)
                .orElse(UserAiPreference.builder().maxOrderAmount(new BigDecimal("5000.00")).build());

        if (amount != null && amount.compareTo(prefs.getMaxOrderAmount()) > 0) {
            throw new IllegalStateException("Sipariş tutarı AI limitini aşıyor (Maks: " + prefs.getMaxOrderAmount() + " TL)");
        }
    }

    /**
     * Saatlik agresif kullanım (spam) kontrolü.
     */
    @Transactional
    public void checkHourlyLimit(Long userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourlyCount = agentTransactionRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
        
        // Sabit limit: Saatte en fazla 30 işlem (spam koruması)
        if (hourlyCount >= 30) {
            throw new IllegalStateException("Çok fazla AI işlemi denediniz. Lütfen daha sonra tekrar deneyin.");
        }
    }

    /**
     * Otomatik onay kontrollerini yapar.
     */
    @Transactional
    public boolean canAutoApprove(Long userId, String actionCategory, BigDecimal amount) {
        UserAiPreference prefs = userAiPreferenceRepository.findByUserId(userId).orElse(null);
        if (prefs == null || !prefs.getAutoApproveEnabled()) {
            return false;
        }

        // Tutar kontrolü
        if (amount != null && prefs.getAutoApproveMaxAmount() != null) {
            if (amount.compareTo(prefs.getAutoApproveMaxAmount()) > 0) {
                return false;
            }
        }

        // Kategori kontrolü
        if (prefs.getAutoApproveCategories() != null && actionCategory != null) {
            try {
                java.util.List<String> categories = objectMapper.readValue(prefs.getAutoApproveCategories(), new TypeReference<List<String>>() {});
                return categories.contains(actionCategory);
            } catch (Exception e) {
                log.error("Failed to parse auto approve categories", e);
                return false;
            }
        }

        return false;
    }

    /**
     * Token'ı kullanıldı (APPROVED) olarak işaretler.
     */
    @Transactional
    public void markTokenAsUsed(String token, Long transactionId) {
        PendingApproval approval = pendingApprovalRepository.findByApprovalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Approval token not found"));
        
        approval.setStatus(PendingApproval.ApprovalStatus.APPROVED);
        approval.setRespondedAt(LocalDateTime.now());
        // Link to the newly created transaction if provided
        if (transactionId != null) {
            agentTransactionRepository.findById(transactionId).ifPresent(approval::setTransaction);
        }
    }
}
