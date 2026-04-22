package com.shopai.controller;

import com.shopai.dto.response.ProductResponses.ProductSummaryResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Satıcı özel işlemleri, istatistik ve ürün yönetimi")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final ProductService productService;

    @GetMapping("/stats")
    @Operation(summary = "Satıcıya özel istatistikleri getir")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(productService.getSellerStats(auth.getUserId()));
    }

    @GetMapping("/products")
    @Operation(summary = "Satıcının ürünlerini listele")
    public ResponseEntity<Page<ProductSummaryResponse>> getMyProducts(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getSellerProducts(auth.getUserId(), page, size));
    }
}
