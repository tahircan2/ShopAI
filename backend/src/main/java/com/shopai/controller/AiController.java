package com.shopai.controller;

import com.shopai.dto.request.AiRequests.ChatRequest;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI chatbot işlemleri")
public class AiController {

    private final AiService aiService;

    /**
     * AI Chat endpoint — Public+ (anonim kullanıcılar da kullanabilir).
     *
     * ⚠️ Güvenlik:
     * - userId ASLA request body'den alınmaz.
     * - JWT cookie mevcutsa userId oradan extract edilir (@AuthenticationPrincipal nullable).
     * - Python servise X-Authenticated-User-Id header'ı ile iletilir.
     */
    @PostMapping("/chat")
    @Operation(summary = "Chatbot mesajı gönder — userId JWT'den alınır, body'den asla")
    public ResponseEntity<Map<String, Object>> chat(
            @AuthenticationPrincipal JwtAuthDetails authDetails, // nullable — anonim kullanıcı
            @Valid @RequestBody ChatRequest req,
            HttpServletRequest request) {

        // userId null olabilir (anonim kullanıcı). JWT mevcutsa oradan alınır.
        Long userId = authDetails != null ? authDetails.getUserId() : null;
        String ip = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();

        return ResponseEntity.ok(aiService.chat(req, userId, ip, request));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chatbot mesajı gönder (Streaming)")
    public reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent<String>> chatStream(
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            @Valid @RequestBody ChatRequest req,
            HttpServletRequest request) {

        Long userId = authDetails != null ? authDetails.getUserId() : null;
        String ip = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();

        return aiService.chatStream(req, userId, ip, request);
    }

    @GetMapping("/conversations/{sessionId}")
    @Operation(summary = "Konuşma geçmişi — ownership check yapılır")
    public ResponseEntity<Map<String, Object>> getConversation(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable String sessionId) {
        // ownership check: yalnızca JWT sahibinin konuşması döndürülür
        return ResponseEntity.ok(aiService.getConversation(sessionId, auth.getUserId()));
    }

    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "Konuşmayı temizle — ownership check yapılır")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable String sessionId) {
        aiService.deleteConversation(sessionId, auth.getUserId());
        return ResponseEntity.noContent().build();
    }
}
