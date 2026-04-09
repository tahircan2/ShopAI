package com.shopai.service;

import com.shopai.dto.request.AiRequests.ChatRequest;
import com.shopai.entity.AiConversation;
import com.shopai.entity.AiMessage;
import com.shopai.entity.User;
import com.shopai.repository.AiConversationRepository;
import com.shopai.repository.AiMessageRepository;
import com.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Value("${app.ai-service.internal-key}")
    private String internalKey;

    /**
     * Angular → Spring Boot → Python AI proxy.
     *
     * ⚠️ Güvenlik:
     * 1. userId mesaj body'sinden ASLA alınmaz — JWT cookie'den extract edilir.
     * 2. Mesaj içeriği sanitize edilir (max 500 karakter, özel karakterler escape).
     * 3. Python servise X-Authenticated-User-Id header'ı ile userId iletilir.
     * 4. Python servisi bu header'a güvenir; başka kaynaktan userId kabul etmez.
     */
    @Transactional
    public Map<String, Object> chat(ChatRequest req, Long userId, String ip) {
        // Mesaj sanitizasyonu — Spring Boot katmanı
        String sanitizedMessage = sanitizeMessage(req.getMessage());

        // Konuşma kaydı al veya oluştur
        AiConversation conversation = getOrCreateConversation(req.getSessionId(), userId);

        // Kullanıcı mesajını DB'ye kaydet
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessage.MessageRole.user)
                .content(sanitizedMessage)
                .build();
        messageRepository.save(userMessage);

        // Konuşma metriklerini güncelle
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        // Python AI servisine proxy — userId'yi X-Authenticated-User-Id header'ı ile ilet
        Map<String, Object> pythonResponse;
        try {
            pythonResponse = webClientBuilder.build()
                    .post()
                    .uri(aiServiceUrl + "/chat")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Authenticated-User-Id", userId != null ? String.valueOf(userId) : "")
                    .header("X-Internal-Key", internalKey) // servisler arası kimlik doğrulama
                    .bodyValue(buildPythonPayload(sanitizedMessage, req.getSessionId(), userId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI service error: {} {}", e.getStatusCode(), e.getMessage());
            pythonResponse = Map.of(
                    "message", "Şu anda yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.",
                    "agentType", "fallback",
                    "injectionDetected", false
            );
        } catch (Exception e) {
            log.error("AI service unreachable: {}", e.getMessage());
            pythonResponse = Map.of(
                    "message", "AI servisi şu anda kullanılamıyor.",
                    "agentType", "fallback",
                    "injectionDetected", false
            );
        }

        // Injection tespiti — audit log
        boolean injectionDetected = Boolean.TRUE.equals(pythonResponse.get("injectionDetected"));
        if (injectionDetected) {
            auditLogService.log(userId, "INJECTION_DETECTED", "AiConversation",
                    conversation.getId(), ip, null);
            log.warn("Prompt injection detected! userId={}, sessionId={}", userId, req.getSessionId());
        }

        // Asistan yanıtını DB'ye kaydet
        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessage.MessageRole.assistant)
                .content(String.valueOf(pythonResponse.getOrDefault("message", "")))
                .agentType(String.valueOf(pythonResponse.getOrDefault("agentType", "")))
                .actionType(pythonResponse.get("actionType") != null
                        ? String.valueOf(pythonResponse.get("actionType")) : null)
                .isInjectionDetected(injectionDetected)
                .build();
        messageRepository.save(assistantMessage);

        // Konuşma sayacını güncelle
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        return pythonResponse instanceof HashMap ? pythonResponse : new HashMap<>(pythonResponse);
    }

    // ─── Konuşma Geçmişi (ownership check) ──────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getConversation(String sessionId, Long userId) {
        AiConversation conversation = conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new com.shopai.exception.ResourceNotFoundException(
                        "Konuşma bulunamadı veya erişim yetkiniz yok"));

        var messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(m -> Map.of(
                        "role", m.getRole().name(),
                        "content", m.getContent(),
                        "agentType", m.getAgentType() != null ? m.getAgentType() : "",
                        "createdAt", m.getCreatedAt().toString()
                ))
                .toList();

        return Map.of(
                "sessionId", sessionId,
                "messageCount", conversation.getMessageCount(),
                "messages", messages
        );
    }

    // ─── Konuşmayı Sil (ownership check) ────────────────────────────────────
    @Transactional
    public void deleteConversation(String sessionId, Long userId) {
        AiConversation conversation = conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new com.shopai.exception.ResourceNotFoundException(
                        "Konuşma bulunamadı veya erişim yetkiniz yok"));
        conversationRepository.delete(conversation);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private AiConversation getOrCreateConversation(String sessionId, Long userId) {
        return conversationRepository.findBySessionId(sessionId).orElseGet(() -> {
            AiConversation conv = AiConversation.builder().sessionId(sessionId).build();
            if (userId != null) {
                userRepository.findById(userId).ifPresent(conv::setUser);
            }
            return conversationRepository.save(conv);
        });
    }

    /**
     * Mesaj sanitizasyonu — Spring Boot katmanı (Katman 2).
     * Max 500 karakter, HTML/script tag temizleme, userId claim silme.
     */
    private String sanitizeMessage(String message) {
        if (message == null) return "";
        return message
                .trim()
                .substring(0, Math.min(message.trim().length(), 500))
                .replaceAll("<[^>]*>", "")              // HTML tag'leri kaldır
                .replaceAll("(?i)userId\\s*[:=]\\s*\\S+", "") // userId inject girişimini temizle
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""); // control characters
    }

    private Map<String, Object> buildPythonPayload(String message, String sessionId, Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("session_id", sessionId);
        // userId Python'a payload'dan DEĞİL — header'dan iletilir
        // Bu satır sadece loglama/tracing için session bağlamı sağlar
        return payload;
    }
}
