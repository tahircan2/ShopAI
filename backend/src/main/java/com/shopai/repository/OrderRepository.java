package com.shopai.repository;

import com.shopai.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber AND o.user.id = :userId")
    Optional<Order> findByOrderNumberAndUserIdWithItems(String orderNumber, Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(String orderNumber);

    // ─── Stats queries ───────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> 'CANCELLED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);

    // Seller stats
    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi " +
           "JOIN oi.product p WHERE p.seller.id = :sellerId AND oi.order.status <> 'CANCELLED'")
    BigDecimal sumRevenueForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi " +
           "JOIN oi.product p WHERE p.seller.id = :sellerId")
    long countOrdersForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi " +
           "JOIN oi.product p WHERE p.seller.id = :sellerId AND oi.order.status = 'PENDING'")
    long countPendingOrdersForSeller(@Param("sellerId") Long sellerId);

    // Date-based stats
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> 'CANCELLED' AND o.createdAt >= :date")
    BigDecimal sumTotalRevenueAfter(@Param("date") java.time.LocalDateTime date);

    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi " +
           "JOIN oi.product p WHERE p.seller.id = :sellerId AND oi.order.status <> 'CANCELLED' AND oi.order.createdAt >= :date")
    BigDecimal sumRevenueForSellerAfter(@Param("sellerId") Long sellerId, @Param("date") java.time.LocalDateTime date);

    @Query("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi " +
           "JOIN oi.product p WHERE p.seller.id = :sellerId AND oi.order.createdAt >= :date")
    long countOrdersForSellerAfter(@Param("sellerId") Long sellerId, @Param("date") java.time.LocalDateTime date);
}
