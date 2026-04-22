package com.shopai.controller;

import com.shopai.dto.request.CartRequests.ApplyCouponRequest;
import com.shopai.dto.response.CartResponse;
import com.shopai.security.JwtAuthDetails;
import com.shopai.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.shopai.service.CouponService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Agent Özel Kupon İşlemleri.
 * Normal kupon akışına ek olarak AI agent üzerinden gelen özel kontrolleri içerebilir.
 */
@RestController
@RequestMapping("/api/agent/coupons")
@RequiredArgsConstructor
@Tag(name = "Agent Coupon", description = "AI Agent üzerinden kupon kullanımı")
public class AgentCouponController {

    private final CartService cartService;
    private final CouponService couponService;

    @GetMapping("/applicable")
    @Operation(summary = "Mevcut uygulanabilir kuponları listele")
    public ResponseEntity<List<Map<String, Object>>> getApplicableCoupons() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @PostMapping("/apply")
    @Operation(summary = "Agent tarafından önerilen kuponu uygula")
    public ResponseEntity<CartResponse> apply(@AuthenticationPrincipal JwtAuthDetails auth,
                                            @Valid @RequestBody ApplyCouponRequest req,
                                            HttpServletRequest request) {
        return ResponseEntity.ok(cartService.applyCoupon(auth.getUserId(), req, request));
    }
}
