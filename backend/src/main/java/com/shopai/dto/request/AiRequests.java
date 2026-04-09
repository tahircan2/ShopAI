package com.shopai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AiRequests {

    @Data
    public static class ChatRequest {
        @NotBlank(message = "Session ID zorunludur")
        private String sessionId;

        @NotBlank(message = "Mesaj zorunludur")
        @Size(max = 500, message = "Mesaj 500 karakteri geçemez")  // sanitizasyon / injection yüzey alanı kısıtlaması
        private String message;

        // userId burada YOK — backend JWT cookie'den alır, kullanıcı giremez
    }
}
