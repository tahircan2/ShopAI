package com.shopai.controller;

import com.shopai.dto.request.AiRequests.ChatRequest;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI Asistan sohbet ve akış yönetimi")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "AI asistan ile sohbet et (Bloklayan)")
    public ResponseEntity<Map<String, Object>> chat(@AuthenticationPrincipal JwtAuthDetails auth,
                                                  @Valid @RequestBody ChatRequest req,
                                                  HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();
        
        Long userId = auth != null ? auth.getUserId() : null;
        String userRole = auth != null ? auth.getRole() : null;
        return ResponseEntity.ok(aiService.chat(req, userId, userRole, ip, request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI asistan ile sohbet et (SSE Akışı)")
    public Flux<ServerSentEvent<String>> chatStream(@AuthenticationPrincipal JwtAuthDetails auth,
                                                  @Valid @RequestBody ChatRequest req,
                                                  HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();

        Long userId = auth != null ? auth.getUserId() : null;
        String userRole = auth != null ? auth.getRole() : null;
        return aiService.chatStream(req, userId, userRole, ip, request);
    }

    @GetMapping("/conversations")
    @Operation(summary = "Kullanıcının tüm konuşmalarını listele")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getConversations(@AuthenticationPrincipal JwtAuthDetails auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiService.listConversations(auth.getUserId()));
    }

    @GetMapping("/conversation/{sessionId}")
    @Operation(summary = "Konuşma geçmişini getir")
    public ResponseEntity<Map<String, Object>> getConversation(@AuthenticationPrincipal JwtAuthDetails auth,
                                                             @PathVariable String sessionId) {
        // Geçmiş sadece giriş yapmış kullanıcılar için tutulur
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiService.getConversation(sessionId, auth.getUserId()));
    }

    @DeleteMapping("/conversation/{sessionId}")
    @Operation(summary = "Konuşma geçmişini sil")
    public ResponseEntity<Void> deleteConversation(@AuthenticationPrincipal JwtAuthDetails auth,
                                                 @PathVariable String sessionId) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        aiService.deleteConversation(sessionId, auth.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/feedback/bug-report")
    @Operation(summary = "Hata bildiriminde bulun")
    public ResponseEntity<Void> submitBugReport(@AuthenticationPrincipal JwtAuthDetails auth,
                                              @RequestBody Map<String, Object> req) {
        Long userId = auth != null ? auth.getUserId() : null;
        // In simple terms, log it securely and return success (Phase 7 mechanism).
        org.slf4j.LoggerFactory.getLogger(AiController.class)
                .warn("[AI_BUG_REPORT] Beklenmeyen hata raporu alındı. User ID: {}, Detaylar: {}", userId, req);
        return ResponseEntity.ok().build();
    }
}
