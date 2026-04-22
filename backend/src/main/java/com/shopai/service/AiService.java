package com.shopai.service;

import com.shopai.dto.request.AiRequests.ChatRequest;
import com.shopai.entity.AiConversation;
import com.shopai.entity.AiMessage;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public Map<String, Object> chat(ChatRequest req, Long userId, String userRole, String ip, jakarta.servlet.http.HttpServletRequest request) {
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
                    .header("X-Authenticated-User-Role", userRole != null ? userRole : "")
                    .header("X-Internal-Key", internalKey) // servisler arası kimlik doğrulama
                    .bodyValue(buildPythonPayload(sanitizedMessage, req.getSessionId(), userId, conversation))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
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
        // Not: Python artık camelCase döner (injectionDetected) ama backward-compatible kalıyoruz
        boolean injectionDetected = Boolean.TRUE.equals(
                pythonResponse.getOrDefault("injectionDetected",
                        pythonResponse.get("injection_detected")));
        if (injectionDetected) {
            // Güvenlik: Ham string'i JSON kolonuna basmak hata vereceği için Map içine alıyoruz
            Map<String, String> logData = Map.of("blockedResponse", String.valueOf(pythonResponse.get("message")));
            auditLogService.logWithMap(userId, "INJECTION_DETECTED", "AiConversation",
                    conversation.getId(), logData, null, ip, request.getHeader("User-Agent"));
            log.warn("Prompt injection detected! userId={}, sessionId={}", userId, req.getSessionId());
        }

        // Asistan yanıtını DB'ye kaydet
        // Backward-compatible: hem camelCase hem snake_case key'leri kontrol et
        String agentTypeVal = getStringFromMap(pythonResponse, "agentType", "agent_type");
        String actionTypeVal = getStringFromMap(pythonResponse, "actionType", "action_type");

        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessage.MessageRole.assistant)
                .content(String.valueOf(pythonResponse.getOrDefault("message", "")))
                .agentType(agentTypeVal)
                .actionType(actionTypeVal)
                .isInjectionDetected(injectionDetected)
                .build();
        messageRepository.save(assistantMessage);

        // Konuşma sayacını güncelle
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        // AI bir aksiyon aldıysa (sepete ekle vs.) bunu audit log'a işle
        if (actionTypeVal != null && !actionTypeVal.equalsIgnoreCase("none")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionData = (Map<String, Object>) pythonResponse.get("actionData");
            auditLogService.logEntityAction(userId, "AI_ACTION_" + actionTypeVal.toUpperCase(), 
                    null, actionData, "AiAction", conversation.getId(), request);
        }

        return pythonResponse instanceof HashMap ? pythonResponse : new HashMap<>(pythonResponse);
    }

    /**
     * AI Streaming işlemi. Frontend SSE akışını buradan okur.
     */
    public Flux<ServerSentEvent<String>> chatStream(ChatRequest req, Long userId, String userRole, String ip, jakarta.servlet.http.HttpServletRequest request) {
        String sanitizedMessage = sanitizeMessage(req.getMessage());
        AiConversation conversation = getOrCreateConversation(req.getSessionId(), userId);

        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessage.MessageRole.user)
                .content(sanitizedMessage)
                .build();
        messageRepository.save(userMessage);

        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        return webClientBuilder.build()
                .post()
                .uri(aiServiceUrl + "/chat/stream")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-Authenticated-User-Id", userId != null ? String.valueOf(userId) : "")
                .header("X-Authenticated-User-Role", userRole != null ? userRole : "")
                .header("X-Internal-Key", internalKey)
                .bodyValue(buildPythonPayload(sanitizedMessage, req.getSessionId(), userId, conversation))
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(sse -> {
                    try {
                        String data = sse.data();
                        if (data != null) {
                            JsonNode node = objectMapper.readTree(data);
                            if (node.has("type") && "state".equals(node.get("type").asText())) {
                                JsonNode state = node.get("state");
                                saveAssistantMessageAndAudit(conversation, state, userId, ip, request);
                            }
                        }
                    } catch (Exception e) {
                        log.error("AI stream parse error", e);
                    }
                })
                .onErrorResume(e -> {
                    log.error("AI service error: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    private void saveAssistantMessageAndAudit(AiConversation conversation, JsonNode state, Long userId, String ip, jakarta.servlet.http.HttpServletRequest request) {
        boolean injectionDetected = state.has("injection_detected") && state.get("injection_detected").asBoolean();
        if (injectionDetected) {
            Map<String, String> logData = Map.of("blockedResponse", state.get("message").asText());
            auditLogService.logWithMap(userId, "INJECTION_DETECTED", "AiConversation",
                    conversation.getId(), logData, null, ip, request.getHeader("User-Agent"));
            log.warn("Prompt injection detected! userId={}, sessionId={}", userId, conversation.getSessionId());
        }

        String actionType = state.has("action_type") && !state.get("action_type").isNull() ? state.get("action_type").asText() : null;
        String agentType = state.has("agent_type") && !state.get("agent_type").isNull() ? state.get("agent_type").asText() : null;

        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation)
                .role(AiMessage.MessageRole.assistant)
                .content(state.has("message") ? state.get("message").asText() : "")
                .agentType(agentType)
                .actionType(actionType)
                .isInjectionDetected(injectionDetected)
                .build();
        messageRepository.save(assistantMessage);

        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversationRepository.save(conversation);

        if (actionType != null && !actionType.equalsIgnoreCase("none") && state.has("action_data")) {
            Map<String, Object> actionData = objectMapper.convertValue(state.get("action_data"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            auditLogService.logEntityAction(userId, "AI_ACTION_" + actionType.toUpperCase(), 
                    null, actionData, "AiAction", conversation.getId(), request);
        }
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

    private Map<String, Object> buildPythonPayload(String message, String sessionId, Long userId, AiConversation conversation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("session_id", sessionId);

        // Geçmişi yükle (son 10 mesaj)
        var history = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversation.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        
        java.util.List<Map<String, String>> historyList = new java.util.ArrayList<>();
        // Mesajları kronolojik sıraya sok (Desc -> Asc)
        for (int i = history.size() - 1; i >= 0; i--) {
            var msg = history.get(i);
            // Mevcut mesajı dahil etme (zaten 'message' alanında gidiyor)
            if (msg.getContent().equals(message)) continue;
            
            historyList.add(Map.of(
                "role", msg.getRole().name(),
                "content", msg.getContent()
            ));
        }
        
        payload.put("conversation_history", historyList);
        return payload;
    }

    /**
     * Backward-compatible map key okuma.
     * Python artık camelCase döner ama geçiş döneminde her iki formatı da destekler.
     * Önce camelCase'i dener, bulamazsa snake_case'e düşer.
     */
    private String getStringFromMap(Map<String, Object> map, String camelKey, String snakeKey) {
        Object val = map.get(camelKey);
        if (val == null) val = map.get(snakeKey);
        return val != null ? String.valueOf(val) : null;
    }
}
