package com.shopai.controller;

import com.shopai.dto.request.ProductRequests.*;
import com.shopai.dto.response.ProductResponses.*;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Ürün listeleme, detay, arama ve yorum yönetimi")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Ürünleri listele (filtreli ve sayfalı)")
    public ResponseEntity<Page<ProductSummaryResponse>> getProducts(ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.getProducts(filter));
    }

    @GetMapping("/featured")
    @Operation(summary = "Öne çıkan ürünleri getir")
    public ResponseEntity<List<ProductSummaryResponse>> getFeatured() {
        return ResponseEntity.ok(productService.getFeatured());
    }

    @GetMapping("/search")
    @Operation(summary = "Ürün ara (typo-tolerant)")
    public ResponseEntity<Page<ProductSummaryResponse>> search(@RequestParam String q,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.search(q, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ürün detayı getir (ID ile)")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Ürün detayı getir (Slug ile)")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    // ─── Yorumlar ────────────────────────────────────────────────────────────
    @GetMapping("/{id}/reviews")
    @Operation(summary = "Ürün yorumlarını listele")
    public ResponseEntity<Page<ReviewResponse>> getReviews(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getReviews(id, page, size));
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Ürüne yorum ekle")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable Long id,
                                                  @AuthenticationPrincipal JwtAuthDetails auth,
                                                  @Valid @RequestBody ReviewRequest req,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok(productService.addReview(id, auth.getUserId(), req, request));
    }

    @DeleteMapping("/{id}/reviews/{reviewId}")
    @Operation(summary = "Yorum sil")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                           @PathVariable Long reviewId,
                                           @AuthenticationPrincipal JwtAuthDetails auth,
                                           HttpServletRequest request) {
        boolean isAdmin = "ROLE_ADMIN".equals(auth.getRole());
        productService.deleteReview(id, reviewId, auth.getUserId(), isAdmin, request);
        return ResponseEntity.ok().build();
    }

    // ─── Admin / Seller CRUD ────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Yeni ürün oluştur (Admin/Seller)")
    public ResponseEntity<ProductResponse> createProduct(@AuthenticationPrincipal JwtAuthDetails auth,
                                                       @Valid @RequestBody CreateProductRequest req,
                                                       HttpServletRequest request) {
        return ResponseEntity.ok(productService.createProduct(req, auth.getUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün güncelle (Admin/Seller)")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                       @AuthenticationPrincipal JwtAuthDetails auth,
                                                       @Valid @RequestBody UpdateProductRequest req,
                                                       HttpServletRequest request) {
        boolean isAdmin = "ROLE_ADMIN".equals(auth.getRole());
        return ResponseEntity.ok(productService.updateProduct(id, req, auth.getUserId(), isAdmin, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün sil (Admin/Seller)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id,
                                            @AuthenticationPrincipal JwtAuthDetails auth,
                                            HttpServletRequest request) {
        boolean isAdmin = "ROLE_ADMIN".equals(auth.getRole());
        productService.deleteProduct(id, auth.getUserId(), isAdmin, request);
        return ResponseEntity.ok().build();
    }

    // ─── Stats ───────────────────────────────────────────────────────────────
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Genel admin istatistikleri")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(productService.getAdminStats());
    }

    @GetMapping("/seller/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Seller özel istatistikleri")
    public ResponseEntity<Map<String, Object>> getSellerStats(@AuthenticationPrincipal JwtAuthDetails auth) {
        return ResponseEntity.ok(productService.getSellerStats(auth.getUserId()));
    }
}
