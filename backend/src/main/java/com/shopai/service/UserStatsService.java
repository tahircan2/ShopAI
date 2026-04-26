package com.shopai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Individual User dashboard istatistik servisi.
 * Tüm veriler JWT'den alınan userId ile scope edilir.
 * userId request'ten ASLA alınmaz.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatsService {

        @PersistenceContext
        private EntityManager em;

        // ─── Personal Stats (KPI kartları) ───────────────────────────────────────

        public Map<String, Object> getPersonalStats(Long userId) {
                // Toplam harcama
                BigDecimal totalSpend = (BigDecimal) em.createQuery(
                                "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                                                "WHERE o.user.id = :uid AND o.status <> 'CANCELLED'")
                                .setParameter("uid", userId)
                                .getSingleResult();

                // Sipariş sayısı
                Long totalOrders = (Long) em.createQuery(
                                "SELECT COUNT(o) FROM Order o WHERE o.user.id = :uid AND o.status <> 'CANCELLED'")
                                .setParameter("uid", userId)
                                .getSingleResult();

                // Ortalama sipariş değeri
                BigDecimal avgOrder = totalOrders > 0
                                ? totalSpend.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Membership seviyesi hesapla
                String membership;
                double progress;
                BigDecimal nextThreshold;

                if (totalSpend.compareTo(BigDecimal.valueOf(2000)) >= 0) {
                        membership = "Gold";
                        progress = 100;
                        nextThreshold = BigDecimal.ZERO;
                } else if (totalSpend.compareTo(BigDecimal.valueOf(500)) >= 0) {
                        membership = "Silver";
                        progress = totalSpend.subtract(BigDecimal.valueOf(500))
                                        .divide(BigDecimal.valueOf(1500), 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100;
                        nextThreshold = BigDecimal.valueOf(2000).subtract(totalSpend);
                } else {
                        membership = "Bronze";
                        progress = totalSpend.divide(BigDecimal.valueOf(500), 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100;
                        nextThreshold = BigDecimal.valueOf(500).subtract(totalSpend);
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("totalSpend", totalSpend);
                result.put("totalOrders", totalOrders);
                result.put("avgOrderValue", avgOrder);
                result.put("membership", membership);
                result.put("membershipProgress", Math.round(progress * 10.0) / 10.0);
                result.put("nextThreshold", nextThreshold);
                return result;
        }

        // ─── Monthly Spend (line chart) ──────────────────────────────────────────
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> getMonthlySpend(Long userId) {
                List<Object[]> rows = em.createQuery(
                                "SELECT FUNCTION('YEAR', o.createdAt), FUNCTION('MONTH', o.createdAt), " +
                                                "COALESCE(SUM(o.totalAmount), 0) " +
                                                "FROM Order o WHERE o.user.id = :uid AND o.status <> 'CANCELLED' " +
                                                "GROUP BY FUNCTION('YEAR', o.createdAt), FUNCTION('MONTH', o.createdAt) "
                                                +
                                                "ORDER BY FUNCTION('YEAR', o.createdAt), FUNCTION('MONTH', o.createdAt)")
                                .setParameter("uid", userId)
                                .getResultList();

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        result.add(Map.of(
                                        "year", row[0],
                                        "month", row[1],
                                        "spend", row[2]));
                }
                return result;
        }

        // ─── Category Spend (donut chart) ────────────────────────────────────────
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> getCategorySpend(Long userId) {
                List<Object[]> rows = em.createQuery(
                                "SELECT c.name, COALESCE(SUM(oi.totalPrice), 0) " +
                                                "FROM OrderItem oi JOIN oi.product p JOIN p.category c JOIN oi.order o "
                                                +
                                                "WHERE o.user.id = :uid AND o.status <> 'CANCELLED' " +
                                                "GROUP BY c.name ORDER BY SUM(oi.totalPrice) DESC")
                                .setParameter("uid", userId)
                                .getResultList();

                BigDecimal total = rows.stream()
                                .map(r -> (BigDecimal) r[1])
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        BigDecimal spend = (BigDecimal) row[1];
                        double pct = total.compareTo(BigDecimal.ZERO) > 0
                                        ? spend.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100
                                        : 0;
                        result.add(Map.of(
                                        "category", row[0],
                                        "spend", spend,
                                        "percentage", Math.round(pct * 10.0) / 10.0));
                }
                return result;
        }

        // ─── Recent Orders ───────────────────────────────────────────────────────
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> getRecentOrders(Long userId, int limit) {
                List<Object[]> rows = em.createQuery(
                                "SELECT o.orderNumber, o.createdAt, SIZE(o.items), o.totalAmount, o.status " +
                                                "FROM Order o WHERE o.user.id = :uid " +
                                                "ORDER BY o.createdAt DESC")
                                .setParameter("uid", userId)
                                .setMaxResults(limit)
                                .getResultList();

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object[] row : rows) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("orderNumber", row[0]);
                        map.put("createdAt", row[1].toString());
                        map.put("itemCount", row[2]);
                        map.put("totalAmount", row[3]);
                        map.put("status", row[4].toString());
                        result.add(map);
                }
                return result;
        }
}
