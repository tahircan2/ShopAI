package com.shopai.service;

import com.shopai.entity.Notification;
import com.shopai.entity.User;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.NotificationRepository;
import com.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    public void sendOrderStatusNotification(Long userId, String orderNumber, String status, Long orderId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String title = "Sipariş Durumu Güncellendi";
        String message = switch (status) {
            case "CONFIRMED"  -> "Siparişiniz #" + orderNumber + " onaylandı.";
            case "SHIPPED"    -> "Siparişiniz #" + orderNumber + " kargoya verildi.";
            case "DELIVERED"  -> "Siparişiniz #" + orderNumber + " teslim edildi.";
            case "CANCELLED"  -> "Siparişiniz #" + orderNumber + " iptal edildi.";
            case "REFUNDED"   -> "Siparişiniz #" + orderNumber + " için iade işlemi başlatıldı.";
            default           -> "Siparişiniz #" + orderNumber + " güncellendi: " + status;
        };

        Notification notification = Notification.builder()
                .user(user)
                .type(Notification.NotificationType.ORDER_STATUS)
                .title(title)
                .message(message)
                .referenceId(orderId)
                .build();
        notificationRepository.save(notification);
    }

    @Async
    public void sendSystemNotification(Long userId, String title, String message) {
        userRepository.findById(userId).ifPresent(user -> {
            Notification notification = Notification.builder()
                    .user(user)
                    .type(Notification.NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .build();
            notificationRepository.save(notification);
        });
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim bulunamadı"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    // ─── Inner DTO ───────────────────────────────────────────────────────────
    public record NotificationResponse(
            Long id,
            String type,
            String title,
            String message,
            Boolean isRead,
            Long referenceId,
            String createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getType().name(),
                    n.getTitle(),
                    n.getMessage(),
                    n.getIsRead(),
                    n.getReferenceId(),
                    n.getCreatedAt().toString()
            );
        }
    }
}
