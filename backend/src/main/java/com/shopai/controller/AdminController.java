package com.shopai.controller;

import com.shopai.dto.request.OrderRequests.*;
import com.shopai.dto.response.OrderResponses.*;
import com.shopai.dto.request.ProductRequests.*;
import com.shopai.dto.response.ProductResponses.*;
import com.shopai.dto.response.AuthResponses.UserInfo;

import com.shopai.entity.ProductImage;
import com.shopai.service.CategoryService;
import com.shopai.service.CouponService;
import com.shopai.service.OrderService;
import com.shopai.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import com.shopai.security.JwtAuthDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final com.shopai.service.UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final CouponService couponService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(productService.getAdminStats());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<UserInfo>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, search, role));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        userService.deleteUser(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserInfo> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(id, body.get("role"), request));
    }

    @PutMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserInfo> toggleUserActive(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(userService.toggleUserActive(id, request));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderSummaryResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, req, request));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Page<ProductSummaryResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAllProductsForAdmin(page, size));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @Valid @RequestBody CreateProductRequest req,
            HttpServletRequest request) {
        Long sellerId = "SELLER".equals(auth.getRole()) ? auth.getUserId() : null;
        return ResponseEntity.ok(productService.createProduct(req, sellerId, request));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req,
            HttpServletRequest request) {
        boolean isAdmin = "ADMIN".equals(auth.getRole());
        return ResponseEntity.ok(productService.updateProduct(id, req, auth.getUserId(), isAdmin, request));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal JwtAuthDetails auth,
            @PathVariable Long id,
            HttpServletRequest request) {
        boolean isAdmin = "ADMIN".equals(auth.getRole());
        productService.deleteProduct(id, auth.getUserId(), isAdmin, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/products/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<ProductImage>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") org.springframework.web.multipart.MultipartFile[] files,
            HttpServletRequest request) {
        return ResponseEntity.ok(productService.uploadImages(id, files, request));
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            HttpServletRequest request) {
        productService.deleteImage(id, imageId, request);
        return ResponseEntity.ok().build();
    }

    // ─── Categories ─────────────────────────────────────────────────────────
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllWithChildren());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(req, request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, req, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, HttpServletRequest request) {
        categoryService.deleteCategory(id, request);
        return ResponseEntity.ok().build();
    }

    // ─── Coupons ────────────────────────────────────────────────────────────
    @GetMapping("/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCoupons() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @PostMapping("/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCoupon(@Valid @RequestBody CouponRequest req) {
        return ResponseEntity.ok(couponService.create(req));
    }

    @DeleteMapping("/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok().build();
    }
}
