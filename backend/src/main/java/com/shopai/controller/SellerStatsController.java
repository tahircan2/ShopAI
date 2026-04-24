package com.shopai.controller;

import com.shopai.security.JwtAuthDetails;
import com.shopai.service.SellerStatsService;
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
 * Satıcı (Corporate) analytics endpoint'leri.
 * Tüm endpoint'ler SELLER role ile korunur.
 * sellerId her zaman JWT'den alınır — request'ten ASLA kabul edilmez.
 */
@RestController
@RequestMapping("/api/seller/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Stats", description = "Satıcı özel analitik ve istatistik endpoint'leri")
public class SellerStatsController {

    private final SellerStatsService statsService;

    @GetMapping("/revenue")
    @Operation(summary = "Satıcı toplam geliri ve dönemsel değişim oranı")
    public ResponseEntity<Map<String, Object>> getRevenue(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getRevenue(auth.getUserId(), days));
    }

    @GetMapping("/orders")
    @Operation(summary = "Satıcı sipariş sayısı ve dönemsel değişim oranı")
    public ResponseEntity<Map<String, Object>> getOrders(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getOrders(auth.getUserId()));
    }

    @GetMapping("/customers")
    @Operation(summary = "Satıcıya sipariş veren müşteri sayısı")
    public ResponseEntity<Map<String, Object>> getCustomers(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getCustomers(auth.getUserId()));
    }

    @GetMapping("/avg-rating")
    @Operation(summary = "Satıcı ürünlerinin ortalama puanı")
    public ResponseEntity<Map<String, Object>> getAvgRating(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getAvgRating(auth.getUserId()));
    }

    @GetMapping("/revenue-chart")
    @Operation(summary = "Günlük gelir grafiği (son N gün)")
    public ResponseEntity<List<Map<String, Object>>> getRevenueChart(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statsService.getRevenueChart(auth.getUserId(), days));
    }

    @GetMapping("/category-chart")
    @Operation(summary = "Kategoriye göre satış dağılımı")
    public ResponseEntity<List<Map<String, Object>>> getCategoryChart(
            @AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(statsService.getCategoryChart(auth.getUserId()));
    }

    @GetMapping("/recent-orders")
    @Operation(summary = "En son siparişler")
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statsService.getRecentOrders(auth.getUserId(), limit));
    }

    @GetMapping("/top-products")
    @Operation(summary = "En çok satan ürünler")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(statsService.getTopProducts(auth.getUserId(), limit));
    }
}
