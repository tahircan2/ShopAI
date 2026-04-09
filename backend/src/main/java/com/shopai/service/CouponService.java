package com.shopai.service;

import com.shopai.dto.request.ProductRequests.CouponRequest;
import com.shopai.entity.Coupon;
import com.shopai.exception.ConflictException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll() {
        return couponRepository.findAll().stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "code", c.getCode(),
                "discountType", c.getDiscountType().name(),
                "discountValue", c.getDiscountValue(),
                "minOrderAmount", c.getMinOrderAmount() != null ? c.getMinOrderAmount() : 0,
                "maxUses", c.getMaxUses() != null ? c.getMaxUses() : 0,
                "usedCount", c.getUsedCount(),
                "validFrom", c.getValidFrom(),
                "validUntil", c.getValidUntil(),
                "isActive", c.getIsActive()
        )).toList();
    }

    @Transactional
    public Map<String, Object> create(CouponRequest req) {
        if (couponRepository.findByCodeAndIsActiveTrue(req.getCode()).isPresent()) {
            throw new ConflictException("Bu kupon kodu zaten kullanılıyor: " + req.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(req.getCode().toUpperCase())
                .discountType(Coupon.DiscountType.valueOf(req.getDiscountType()))
                .discountValue(req.getDiscountValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxUses(req.getMaxUses())
                .validFrom(req.getValidFrom())
                .validUntil(req.getValidUntil())
                .isActive(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return Map.of(
                "id", saved.getId(),
                "code", saved.getCode(),
                "discountType", saved.getDiscountType().name(),
                "discountValue", saved.getDiscountValue(),
                "isActive", saved.getIsActive()
        );
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kupon bulunamadı"));
        coupon.setIsActive(false); // soft delete
        couponRepository.save(coupon);
    }
}
