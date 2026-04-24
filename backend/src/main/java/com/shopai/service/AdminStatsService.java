package com.shopai.service;

import com.shopai.entity.AuditLog;
import com.shopai.entity.User;
import com.shopai.repository.AuditLogRepository;
import com.shopai.repository.OrderRepository;
import com.shopai.repository.ProductRepository;
import com.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import java.util.*;

/**
 * Platform geneli Admin istatistik servisi.
 * Tüm endpoint'ler ADMIN role ile korunur (Controller katmanında).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

        private final OrderRepository orderRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final AuditLogRepository auditLogRepository;
        private final EntityManager em;

        // ── Platform Revenue ───────────────────────────────────────────────────

        public Map<String, Object> getPlatformRevenue() {
                BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
                LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
                BigDecimal lastMonthRevenue = orderRepository.sumTotalRevenueAfter(lastMonth);

                LocalDateTime twoMonthsAgo = LocalDateTime.now().minusMonths(2);
                BigDecimal previousMonthRevenue = sumRevenueBetween(twoMonthsAgo, lastMonth);

                double change = calculatePercentChange(previousMonthRevenue, lastMonthRevenue);

                return Map.of(
                                "totalRevenue", totalRevenue,
                                "change", change);
        }

        // ── Users Stats ────────────────────────────────────────────────────────

        public Map<String, Object> getUsersStats() {
                long total = userRepository.count();
                long newThisMonth = userRepository.countByCreatedAtAfter(
                                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));

                // Role dağılımı
                @SuppressWarnings("unchecked")
                List<Object[]> roleCounts = em.createQuery(
                                "SELECT u.role, COUNT(u) FROM User u GROUP BY u.role").getResultList();

                Map<String, Long> byRole = new LinkedHashMap<>();
                for (Object[] row : roleCounts) {
                        byRole.put(((User.Role) row[0]).name(), (Long) row[1]);
                }

                return Map.of(
                                "total", total,
                                "newThisMonth", newThisMonth,
                                "byRole", byRole);
        }

        // ── Stores (Sellers) Stats ──────────────────────────────────────────────

        public Map<String, Object> getStoresStats() {
                @SuppressWarnings("unchecked")
                List<Long> sellerIds = em.createQuery(
                                "SELECT DISTINCT u.id FROM User u WHERE u.role = 'SELLER'").getResultList();

                long total = sellerIds.size();

                // Aktif mağaza = en az 1 aktif ürünü olan seller
                long active = 0;
                if (!sellerIds.isEmpty()) {
                        active = (Long) em.createQuery(
                                        "SELECT COUNT(DISTINCT p.seller.id) FROM Product p WHERE p.seller.id IN :ids AND p.isActive = true")
                                        .setParameter("ids", sellerIds).getSingleResult();
                }

                long newThisMonth = em.createQuery(
                                "SELECT COUNT(u) FROM User u WHERE u.role = 'SELLER' AND u.createdAt >= :date",
                                Long.class)
                                .setParameter("date",
                                                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0)
                                                                .withSecond(0))
                                .getSingleResult();

                return Map.of(
                                "total", total,
                                "active", active,
                                "newThisMonth", newThisMonth);
        }

        // ── Average Order Value ────────────────────────────────────────────────

        public Map<String, Object> getAov() {
                Object[] result = (Object[]) em.createQuery(
                                "SELECT COALESCE(AVG(o.totalAmount), 0), COUNT(o) FROM Order o WHERE o.status <> 'CANCELLED'")
                                .getSingleResult();

                BigDecimal avg = result[0] instanceof BigDecimal ? (BigDecimal) result[0]
                                : BigDecimal.valueOf(((Number) result[0]).doubleValue());

                return Map.of(
                                "avgOrderValue", avg.setScale(2, RoundingMode.HALF_UP),
                                "totalOrders", result[1]);
        }

        // ── Platform Revenue Chart (daily) ─────────────────────────────────────

        public List<Map<String, Object>> getPlatformRevenueChart(int days) {
                LocalDateTime startDate = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);

                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createQuery(
                                "SELECT FUNCTION('DATE', o.createdAt), COALESCE(SUM(o.totalAmount), 0) " +
                                                "FROM Order o WHERE o.status <> 'CANCELLED' AND o.createdAt >= :start "
                                                +
                                                "GROUP BY FUNCTION('DATE', o.createdAt) " +
                                                "ORDER BY FUNCTION('DATE', o.createdAt)")
                                .setParameter("start", startDate).getResultList();

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        result.add(Map.of(
                                        "date", row[0].toString(),
                                        "revenue", row[1]));
                }
                return result;
        }

        // ── Store Comparison (Top 10 Sellers) ──────────────────────────────────

        public List<Map<String, Object>> getStoreComparison() {
                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createQuery(
                                "SELECT p.seller.id, p.seller.firstName, p.seller.lastName, " +
                                                "COALESCE(SUM(oi.totalPrice), 0), COUNT(DISTINCT oi.order.id) " +
                                                "FROM OrderItem oi JOIN oi.product p " +
                                                "WHERE oi.order.status <> 'CANCELLED' AND p.seller IS NOT NULL " +
                                                "GROUP BY p.seller.id, p.seller.firstName, p.seller.lastName " +
                                                "ORDER BY COALESCE(SUM(oi.totalPrice), 0) DESC")
                                .setMaxResults(10).getResultList();

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        long productCount = productRepository.countBySellerId((Long) row[0]);
                        result.add(Map.of(
                                        "sellerId", row[0],
                                        "sellerName", row[1] + " " + row[2],
                                        "revenue", row[3],
                                        "orderCount", row[4],
                                        "productCount", productCount));
                }
                return result;
        }

        // ── User Role Distribution ─────────────────────────────────────────────

        public List<Map<String, Object>> getUserRoleDistribution() {
                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createQuery(
                                "SELECT u.role, COUNT(u) FROM User u GROUP BY u.role").getResultList();

                long total = userRepository.count();
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        long count = (Long) row[1];
                        double percentage = total > 0 ? (count * 100.0 / total) : 0;
                        result.add(Map.of(
                                        "role", ((User.Role) row[0]).name(),
                                        "count", count,
                                        "percentage", Math.round(percentage * 10.0) / 10.0));
                }
                return result;
        }

        // ── Audit Logs ─────────────────────────────────────────────────────────

        public List<Map<String, Object>> getRecentAuditLogs(int limit) {
                @SuppressWarnings("unchecked")
                List<AuditLog> logs = em.createQuery(
                                "SELECT a FROM AuditLog a ORDER BY a.createdAt DESC").setMaxResults(limit)
                                .getResultList();

                List<Map<String, Object>> result = new ArrayList<>();
                for (AuditLog log : logs) {
                        String userEmail = "";
                        if (log.getUserId() != null) {
                                userEmail = userRepository.findById(log.getUserId())
                                                .map(User::getEmail).orElse("unknown");
                        }
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("id", log.getId());
                        entry.put("timestamp", log.getCreatedAt().toString());
                        entry.put("userEmail", userEmail);
                        entry.put("action", log.getAction());
                        entry.put("entityType", log.getEntityType());
                        entry.put("entityId", log.getEntityId());
                        entry.put("ipAddress", log.getIpAddress());
                        entry.put("isAiAction", log.getIsAiAction());
                        result.add(entry);
                }
                return result;
        }

        // ── Toggle Store Status ────────────────────────────────────────────────

        @Transactional
        public void toggleStoreStatus(Long sellerId, String status) {
                User seller = userRepository.findById(sellerId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                if (seller.getRole() != User.Role.SELLER) {
                        throw new RuntimeException("User is not a seller");
                }

                boolean isActive = "OPEN".equalsIgnoreCase(status);
                seller.setIsActive(isActive);
                userRepository.save(seller);

                // Audit log
                AuditLog auditLog = AuditLog.builder()
                                .action("STORE_" + (isActive ? "OPENED" : "CLOSED"))
                                .entityType("STORE")
                                .entityId(sellerId)
                                .newData("{\"status\": \"" + status + "\"}")
                                .isAiAction(false)
                                .build();
                auditLogRepository.save(auditLog);
        }

        // ── Helpers ─────────────────────────────────────────────────────────────

        private BigDecimal sumRevenueBetween(LocalDateTime start, LocalDateTime end) {
                Object result = em.createQuery(
                                "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                                                "WHERE o.status <> 'CANCELLED' AND o.createdAt >= :start AND o.createdAt < :end")
                                .setParameter("start", start).setParameter("end", end).getSingleResult();
                return result instanceof BigDecimal ? (BigDecimal) result
                                : BigDecimal.valueOf(((Number) result).doubleValue());
        }

        private double calculatePercentChange(BigDecimal previous, BigDecimal current) {
                if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
                        return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
                }
                return current.subtract(previous)
                                .divide(previous, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(1, RoundingMode.HALF_UP)
                                .doubleValue();
        }
}
