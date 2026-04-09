package com.shopai.repository;

import com.shopai.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.code = :code")
    void incrementUsedCount(String code);
}
