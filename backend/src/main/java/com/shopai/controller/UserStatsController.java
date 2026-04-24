package com.shopai.controller;

import com.shopai.security.JwtAuthDetails;
import com.shopai.service.UserStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Individual User analytics endpoint'leri.
 * userId JWT'den alınır — request'ten ASLA kabul edilmez.
 */
@RestController
@RequestMapping("/api/user/stats")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Stats", description = "Kişisel analitik ve harcama istatistikleri")
public class UserStatsController {

    private final UserStatsService statsService;

    @GetMapping("/personal")
    @Operation(summary = "Kişisel KPI'lar: toplam harcama, sipariş sayısı, ortalama, membership")
    public ResponseEntity<Map<String, Object>> getPersonalStats(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getPersonalStats(auth.getUserId()));
    }

    @GetMapping("/monthly-spend")
    @Operation(summary = "Aylık harcama trendi (line chart)")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySpend(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getMonthlySpend(auth.getUserId()));
    }

    @GetMapping("/category-spend")
    @Operation(summary = "Kategoriye göre harcama dağılımı (donut chart)")
    public ResponseEntity<List<Map<String, Object>>> getCategorySpend(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getCategorySpend(auth.getUserId()));
    }

    @GetMapping("/recent-orders")
    @Operation(summary = "Son siparişler")
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statsService.getRecentOrders(auth.getUserId(), limit));
    }
}
