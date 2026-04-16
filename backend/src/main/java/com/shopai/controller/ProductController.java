package com.shopai.controller;

import com.shopai.dto.request.ProductRequests.*;
import com.shopai.dto.response.ProductResponses.*;
import com.shopai.entity.Product;
import com.shopai.entity.ProductImage;
import com.shopai.repository.ProductImageRepository;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.CategoryService;
import com.shopai.service.CloudinaryService;
import com.shopai.service.CouponService;
import com.shopai.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Ürün ve kategori işlemleri")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final CloudinaryService cloudinaryService;
    private final ProductImageRepository productImageRepository;

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
            @Valid @RequestBody ReviewRequest req,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addReview(id, authDetails.getUserId(), req, request));
    }

    @DeleteMapping("/api/products/{productId}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Yorum sil (kendi yorumu veya admin)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            jakarta.servlet.http.HttpServletRequest request) {
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        productService.deleteReview(productId, reviewId, authDetails.getUserId(), isAdmin, request);
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
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(req, authDetails.getUserId(), request));
    }

    @PutMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün güncelle")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req,
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            jakarta.servlet.http.HttpServletRequest request) {
        if (authDetails == null) {
            log.error("Unauthorized update attempt for product ID: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Product update request received for ID: {} by user: {}", id, authDetails.getEmail());
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        return ResponseEntity.ok(productService.updateProduct(id, req, authDetails.getUserId(), isAdmin, request));
    }

    @DeleteMapping("/api/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün sil / soft delete (Admin/Seller)")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthDetails authDetails,
            jakarta.servlet.http.HttpServletRequest request) {
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        productService.deleteProduct(id, authDetails.getUserId(), isAdmin, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Product Images (Cloudinary) ─────────────────────────────────────────

    @PostMapping(value = "/api/admin/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürüne resim yükle (Cloudinary)")
    public ResponseEntity<List<ProductImageResponse>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {

        log.info("Image upload request received for product ID: {}, File count: {}", id, files != null ? files.length : 0);
        
        Product product = productService.findActiveById(id);

        if (files == null || files.length == 0) {
            log.warn("No files provided in the request for product ID: {}", id);
            return ResponseEntity.badRequest().build();
        }

        if (authDetails == null) {
            log.error("Unauthorized image upload attempt for product ID: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Authorization: only the product's seller or an admin can upload
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        if (!isAdmin && (product.getSeller() == null || !product.getSeller().getId().equals(authDetails.getUserId()))) {
            log.warn("Unauthorized image upload attempt for product ID: {} by user ID: {}", id, authDetails.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        int currentCount = productImageRepository.countByProductId(id);
        if (currentCount + files.length > 8) {
            log.warn("Product ID {} already has {} images. Cannot add {} more. Max is 8.", id, currentCount, files.length);
            throw new com.shopai.exception.BadRequestException("Bir ürün için en fazla 8 resim yüklenebilir.");
        }

        List<ProductImageResponse> responses = new ArrayList<>();
        String lastError = "Bilinmeyen bir hata oluştu";
        
        for (int i = 0; i < files.length; i++) {
            try {
                log.info("Uploading file {}/{} for product ID: {}", i+1, files.length, id);
                String url = cloudinaryService.uploadImage(files[i]);
                
                if (url == null || url.isEmpty()) {
                    log.error("Cloudinary returned null/empty URL for file: {}", files[i].getOriginalFilename());
                    continue;
                }

                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .altText(product.getName())
                        .isPrimary(currentCount == 0 && i == 0)
                        .sortOrder(currentCount + i)
                        .build();
                productImageRepository.save(image);
                responses.add(ProductImageResponse.from(image));
            } catch (Exception e) {
                lastError = e.getMessage();
                log.error("Failed to process image {} for product ID {}: {}", i, id, lastError);
            }
        }
        
        if (responses.isEmpty()) {
            log.error("All image uploads failed for product ID: {}. Last error: {}", id, lastError);
            // 500 dönmek yerine asıl hatayı fırlatıyoruz (GlobalExceptionHandler bunu 400/500 olarak formatlar)
            throw new com.shopai.exception.BadRequestException("Resimler yüklenemedi: " + lastError);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @DeleteMapping("/api/admin/products/{productId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Ürün resmini sil")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {

        Product product = productService.findActiveById(productId);
        boolean isAdmin = "ADMIN".equals(authDetails.getRole()) || "ROLE_ADMIN".equals(authDetails.getRole());
        if (!isAdmin && (product.getSeller() == null || !product.getSeller().getId().equals(authDetails.getUserId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElse(null);
        if (image == null || !image.getProduct().getId().equals(productId)) {
            return ResponseEntity.notFound().build();
        }

        cloudinaryService.deleteImage(image.getImageUrl());
        productImageRepository.delete(image);
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
