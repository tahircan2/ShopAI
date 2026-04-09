package com.shopai.scheduler;

import com.shopai.entity.Order;
import com.shopai.repository.OrderRepository;
import com.shopai.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Her saat başı PENDING durumunda 24 saatten uzun süre kalan siparişleri kontrol eder.
     * Production'da bu iş sipariş yönetim sistemine entegre edilmeli.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void checkStalePendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        orderRepository.findAll(PageRequest.of(0, 100)).stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING
                        && o.getCreatedAt().isBefore(threshold))
                .forEach(order -> {
                    log.warn("Stale PENDING order detected: {} (userId={})",
                            order.getOrderNumber(), order.getUser().getId());
                    notificationService.sendSystemNotification(
                            order.getUser().getId(),
                            "Siparişiniz Bekleniyor",
                            "Siparişiniz #" + order.getOrderNumber() +
                            " hâlâ onay bekliyor. Sorun yaşıyorsanız destek ile iletişime geçin."
                    );
                });
    }
}
