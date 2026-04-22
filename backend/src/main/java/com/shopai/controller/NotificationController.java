package com.shopai.controller;

import com.shopai.security.JwtAuthDetails;
import com.shopai.service.NotificationService;
import com.shopai.service.NotificationService.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Kullanıcı bildirim yönetimi")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Bildirimleri listele")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(@AuthenticationPrincipal JwtAuthDetails auth,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(auth.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Okunmamış bildirim sayısını getir")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(notificationService.getUnreadCount(auth.getUserId()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Bildirimi okundu olarak işaretle")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal JwtAuthDetails auth,
                                         @PathVariable Long id) {
        notificationService.markAsRead(auth.getUserId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Tüm bildirimleri okundu olarak işaretle")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal JwtAuthDetails auth) {
        notificationService.markAllAsRead(auth.getUserId());
        return ResponseEntity.ok().build();
    }
}
