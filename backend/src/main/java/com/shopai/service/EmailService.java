package com.shopai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmailVerification(String to, String token) {
        String link = frontendUrl + "/auth/verify-email?token=" + token;
        sendSimpleEmail(to, "ShopAI - E-posta Doğrulama",
                "Hesabınızı doğrulamak için aşağıdaki linke tıklayın:\n\n" + link +
                "\n\nBu link 24 saat geçerlidir.\n\nEğer hesap oluşturmadıysanız bu e-postayı görmezden gelin.");
    }

    @Async
    public void sendPasswordReset(String to, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        sendSimpleEmail(to, "ShopAI - Şifre Sıfırlama",
                "Şifrenizi sıfırlamak için aşağıdaki linke tıklayın:\n\n" + link +
                "\n\nBu link 2 saat geçerlidir.\n\nEğer bu isteği siz yapmadıysanız şifreniz güvende, bu e-postayı görmezden gelin.");
    }

    @Async
    public void sendOrderConfirmation(String to, String orderNumber, String totalAmount) {
        sendSimpleEmail(to, "ShopAI - Siparişiniz Alındı #" + orderNumber,
                "Siparişiniz başarıyla alındı!\n\nSipariş No: " + orderNumber +
                "\nToplam: " + totalAmount + " ₺\n\nSiparişinizi hesabınızdan takip edebilirsiniz.");
    }

    @Async
    public void sendOrderStatusUpdate(String to, String orderNumber, String status) {
        sendSimpleEmail(to, "ShopAI - Sipariş Durumu Güncellendi #" + orderNumber,
                "Sipariş #" + orderNumber + " durumunuz güncellendi: " + status);
    }

    private void sendSimpleEmail(String to, String subject, String text) {
        if (to == null || to.trim().isEmpty()) return;

        // Profesyonel Çözüm: Test ve Dummy maillere (Örn: admin@shopai.com) giden SMTP isteklerini kes
        if (to.toLowerCase().endsWith("@shopai.com") || to.toLowerCase().endsWith("@example.com")) {
            log.info("Test/Dummy sistem mailine ({}) gönderim atlandı. (Gereksiz Bounceleri (Teslim Edilemedi) önlemek için)", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email başarıyla gönderildi - To: {}, Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Email gönderilemedi - To: {}, Subject: {}, Error: {}", to, subject, e.getMessage());
        }
    }
}
