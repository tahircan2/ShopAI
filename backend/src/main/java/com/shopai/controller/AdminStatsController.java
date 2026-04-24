package com.shopai.controller;

import com.shopai.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Platform geneli admin analytics endpoint'leri.
 * Tüm endpoint'ler ADMIN role ile korunur.
 *
 * Mevcut AdminController'daki basit /stats endpoint'i korunur,
 * bu controller daha detaylı analytics sağlar.
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Stats", description = "Platform geneli analitik ve istatistik endpoint'leri")
public class AdminStatsController {

    private final AdminStatsService statsService;

    @GetMapping("/platform-revenue")
    @Operation(summary = "Platform toplam geliri ve aylık değişim oranı")
    public ResponseEntity<Map<String, Object>> getPlatformRevenue() {
        return ResponseEntity.ok(statsService.getPlatformRevenue());
    }

    @GetMapping("/users")
    @Operation(summary = "Toplam kullanıcı, yeni kayıtlar ve rol dağılımı")
    public ResponseEntity<Map<String, Object>> getUsersStats() {
        return ResponseEntity.ok(statsService.getUsersStats());
    }

    @GetMapping("/stores")
    @Operation(summary = "Toplam mağaza (satıcı), aktif ve bu ay yeni eklenen")
    public ResponseEntity<Map<String, Object>> getStoresStats() {
        return ResponseEntity.ok(statsService.getStoresStats());
    }

    @GetMapping("/aov")
    @Operation(summary = "Platform geneli ortalama sipariş değeri")
    public ResponseEntity<Map<String, Object>> getAov() {
        return ResponseEntity.ok(statsService.getAov());
    }

    @GetMapping("/platform-revenue-chart")
    @Operation(summary = "Günlük platform gelir grafiği (son N gün)")
    public ResponseEntity<List<Map<String, Object>>> getPlatformRevenueChart(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getPlatformRevenueChart(days));
    }

    @GetMapping("/store-comparison")
    @Operation(summary = "En çok gelir getiren 10 satıcı karşılaştırması")
    public ResponseEntity<List<Map<String, Object>>> getStoreComparison() {
        return ResponseEntity.ok(statsService.getStoreComparison());
    }

    @GetMapping("/user-role-distribution")
    @Operation(summary = "Kullanıcı rol dağılımı (pie chart verisi)")
    public ResponseEntity<List<Map<String, Object>>> getUserRoleDistribution() {
        return ResponseEntity.ok(statsService.getUserRoleDistribution());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "En son audit log kayıtları")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(statsService.getRecentAuditLogs(limit));
    }

    @PutMapping("/stores/{id}/status")
    @Operation(summary = "Mağaza (Satıcı) durumunu aç/kapat")
    public ResponseEntity<Void> toggleStoreStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        statsService.toggleStoreStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
