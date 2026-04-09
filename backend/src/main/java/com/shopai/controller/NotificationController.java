package com.shopai.controller;

import com.shopai.security.JwtAuthDetails;
import com.shopai.service.NotificationService;
import com.shopai.service.NotificationService.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Bildirim işlemleri")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Bildirimler — ownership check: yalnızca JWT sahibinin bildirimleri")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(auth.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Okunmamış bildirim sayısı")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(auth.getUserId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Bildirimi okundu işaretle — ownership check yapılır")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long id) {
        notificationService.markAsRead(auth.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Tüm bildirimleri okundu işaretle")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal JwtAuthDetails auth) {
        notificationService.markAllAsRead(auth.getUserId());
        return ResponseEntity.noContent().build();
    }
}
