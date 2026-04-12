package com.shopai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 0 Maliyetli Observability Çözümü:
 * Beklenmeyen 500 Patlamalarında WhatsApp (CallMeBot)
 * üzerinden anında telefonunuza bildirim fırlatır.
 */
@Slf4j
@Service
public class WebhookAlertService {

    private final WebClient webClient;

    public WebhookAlertService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Value("${app.webhook.whatsapp.phone:}")
    private String whatsappPhone;

    @Value("${app.webhook.whatsapp.apikey:}")
    private String whatsappApiKey;

    @Async
    public void sendErrorAlert(String errorMessage, String stackTrace, String requestUrl) {
        if (whatsappPhone == null || whatsappPhone.isEmpty() || whatsappApiKey == null || whatsappApiKey.isEmpty()) {
            log.warn("WhatsApp Webhook (Telefon veya API Key) ayarlanmamış. Hata bildirimi atlandı.");
            return;
        }

        try {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            
            // CallMeBot formatlaması: *kalın*, _eğik_ 
            String message = String.format(
                    "🚨 *ShopAI KRITIK HATA ALARMI* 🚨\n\n" +
                    "Zaman: %s\n" +
                    " Endpoint: %s\n\n" +
                    "*Hata Mesajı:*\n```%s```\n\n" +
                    "*Stack Trace (Kısa):*\n```%s```",
                    time, requestUrl, errorMessage, truncateString(stackTrace, 600)
            );

            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());

            String apiUrl = String.format("https://api.callmebot.com/whatsapp.php?phone=%s&apikey=%s&text=%s",
                    whatsappPhone, whatsappApiKey, encodedMessage);

            webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        log.error("WhatsApp bildirimi gönderilemedi: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe(); // non-blocking fire and forget

        } catch (Exception e) {
            log.error("WebhookAlertService'de ikincil hata oluştu: {}", e.getMessage());
        }
    }

    private String truncateString(String text, int maxLength) {
        if (text == null) return "null";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
