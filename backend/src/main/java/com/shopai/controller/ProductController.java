package com.shopai.controller;

import com.shopai.dto.request.ProductRequests.*;
import com.shopai.dto.response.ProductResponses.*;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.CategoryService;
import com.shopai.service.CouponService;
import com.shopai.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Ürün ve kategori işlemleri")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CouponService couponService;

    // ─── Public ──────────────────────────────────────────────────────────────

    @GetMapping("/api/products")
    @Operation(summary = "Ürün listesi (filtreli, sayfalı)")
    public ResponseEntity<Page<ProductSummaryResponse>> getProducts(
            @ParameterObject ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.getProducts(filter));
    }

    @GetMapping("/api/products/search")
    @Operation(summary = "Ürün arama")
    public ResponseEntity<Page<ProductSummaryResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.search(q, page, size));
    }

    @GetMapping("/api/products/featured")
    @Operation(summary = "Öne çıkan ürünler")
    public ResponseEntity<List<ProductSummaryResponse>> getFeatured() {
        return ResponseEntity.ok(productService.getFeatured());
    }

    @GetMapping("/api/products/{id}")
    @Operation(summary = "ID ile ürün detayı")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/api/products/slug/{slug}")
    @Operation(summary = "Slug ile ürün detayı (SEO)")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    @GetMapping("/api/products/{id}/reviews")
    @Operation(summary = "Ürün yorumları")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getReviews(id, page, size));
    }

    @PostMapping("/api/products/{id}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Yorum ekle")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            @Valid @RequestBody ReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addReview(id, authDetails.getUserId(), req));
    }

    @DeleteMapping("/api/products/{productId}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Yorum sil (kendi yorumu veya admin)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        productService.deleteReview(productId, reviewId, authDetails.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ─── Categories ──────────────────────────────────────────────────────────

    @GetMapping("/api/categories")
    @Operation(summary = "Tüm kategoriler (ağaç yapısı)")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllWithChildren());
    }

    @GetMapping("/api/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin istatistikleri")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(productService.getAdminStats());
    }

    @PostMapping("/api/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Yeni ürün ekle (Admin/Seller)")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest req,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(req, authDetails.getUserId()));
    }

    @PutMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün güncelle (Admin/Seller)")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        return ResponseEntity.ok(productService.updateProduct(id, req, authDetails.getUserId(), isAdmin));
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün sil / soft delete (Admin/Seller)")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        productService.deleteProduct(id, authDetails.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ─── Seller ──────────────────────────────────────────────────────────────

    @GetMapping("/api/seller/products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Satıcının kendi ürünleri")
    public ResponseEntity<Page<ProductSummaryResponse>> getMyProducts(
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getSellerProducts(authDetails.getUserId(), page, size));
    }

    @GetMapping("/api/seller/stats")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Satıcı istatistikleri")
    public ResponseEntity<Map<String, Object>> getSellerStats(
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        return ResponseEntity.ok(productService.getSellerStats(authDetails.getUserId()));
    }

    // ─── Admin Coupon CRUD ───────────────────────────────────────────────────

    @GetMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tüm kuponlar")
    public ResponseEntity<?> getCoupons() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @PostMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kupon oluştur")
    public ResponseEntity<?> createCoupon(@Valid @RequestBody CouponRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(req));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kupon sil")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
