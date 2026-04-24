package com.shopai.service;

import com.shopai.repository.OrderRepository;
import com.shopai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStatsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EntityManager em;

    public Map<String, Object> getRevenue(Long sellerId, int days) {
        LocalDateTime periodStart = LocalDateTime.now().minusDays(days);
        BigDecimal currentRevenue = orderRepository.sumRevenueForSellerAfter(sellerId, periodStart);
        LocalDateTime prevStart = LocalDateTime.now().minusDays(days * 2L);
        BigDecimal prevRevenue = sumSellerRevenueBetween(sellerId, prevStart, periodStart);
        double change = calcChange(prevRevenue, currentRevenue);
        return Map.of("totalRevenue", currentRevenue, "change", change, "period", days + " days");
    }

    public Map<String, Object> getOrders(Long sellerId) {
        long total = orderRepository.countOrdersForSeller(sellerId);
        LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long thisM = orderRepository.countOrdersForSellerAfter(sellerId, thisMonth);
        long lastM = countSellerOrdersBetween(sellerId, thisMonth.minusMonths(1), thisMonth);
        return Map.of("totalOrders", total, "ordersThisMonth", thisM,
                "change", calcChange(BigDecimal.valueOf(lastM), BigDecimal.valueOf(thisM)));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomers(Long sellerId) {
        Long total = (Long) em.createQuery(
                "SELECT COUNT(DISTINCT o.user.id) FROM Order o JOIN o.items oi JOIN oi.product p WHERE p.seller.id = :sid AND o.status <> 'CANCELLED'"
        ).setParameter("sid", sellerId).getSingleResult();
        LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long newM = (Long) em.createQuery(
                "SELECT COUNT(DISTINCT o.user.id) FROM Order o JOIN o.items oi JOIN oi.product p WHERE p.seller.id = :sid AND o.status <> 'CANCELLED' AND o.createdAt >= :d"
        ).setParameter("sid", sellerId).setParameter("d", thisMonth).getSingleResult();
        return Map.of("totalCustomers", total, "newThisMonth", newM);
    }

    public Map<String, Object> getAvgRating(Long sellerId) {
        Double avg = productRepository.avgRatingForSeller(sellerId);
        return Map.of("avgRating", avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0, "change", 0.0);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRevenueChart(Long sellerId, int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);
        List<Object[]> rows = em.createQuery(
                "SELECT FUNCTION('DATE', o.createdAt), COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi JOIN oi.order o JOIN oi.product p WHERE p.seller.id = :sid AND o.status <> 'CANCELLED' AND o.createdAt >= :s GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY FUNCTION('DATE', o.createdAt)"
        ).setParameter("sid", sellerId).setParameter("s", start).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) result.add(Map.of("date", r[0].toString(), "revenue", r[1]));
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCategoryChart(Long sellerId) {
        List<Object[]> rows = em.createQuery(
                "SELECT c.name, COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi JOIN oi.product p JOIN p.category c WHERE p.seller.id = :sid AND oi.order.status <> 'CANCELLED' GROUP BY c.name ORDER BY COALESCE(SUM(oi.totalPrice), 0) DESC"
        ).setParameter("sid", sellerId).getResultList();
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] r : rows) total = total.add(toBd(r[1]));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            BigDecimal v = toBd(r[1]);
            double pct = total.compareTo(BigDecimal.ZERO) > 0 ? v.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0;
            result.add(Map.of("category", r[0], "revenue", v, "percentage", pct));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentOrders(Long sellerId, int limit) {
        List<Object[]> rows = em.createQuery(
                "SELECT o.orderNumber, o.user.firstName, o.user.lastName, SUM(oi.totalPrice), o.status, o.createdAt, COUNT(oi) FROM OrderItem oi JOIN oi.order o JOIN oi.product p WHERE p.seller.id = :sid GROUP BY o.id, o.orderNumber, o.user.firstName, o.user.lastName, o.status, o.createdAt ORDER BY o.createdAt DESC"
        ).setParameter("sid", sellerId).setMaxResults(limit).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("orderNumber", r[0]); e.put("customerName", r[1] + " " + r[2]);
            e.put("total", r[3]); e.put("status", r[4].toString());
            e.put("createdAt", r[5].toString()); e.put("itemCount", r[6]);
            result.add(e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopProducts(Long sellerId, int limit) {
        List<Object[]> rows = em.createQuery(
                "SELECT p.name, SUM(oi.quantity), SUM(oi.totalPrice), p.ratingAvg FROM OrderItem oi JOIN oi.product p WHERE p.seller.id = :sid AND oi.order.status <> 'CANCELLED' GROUP BY p.id, p.name, p.ratingAvg ORDER BY SUM(oi.quantity) DESC"
        ).setParameter("sid", sellerId).setMaxResults(limit).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) result.add(Map.of("productName", r[0], "totalQuantity", r[1], "totalRevenue", r[2], "rating", r[3] != null ? r[3] : 0));
        return result;
    }

    private BigDecimal sumSellerRevenueBetween(Long sid, LocalDateTime s, LocalDateTime e) {
        Object r = em.createQuery("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi JOIN oi.product p WHERE p.seller.id = :sid AND oi.order.status <> 'CANCELLED' AND oi.order.createdAt >= :s AND oi.order.createdAt < :e")
                .setParameter("sid", sid).setParameter("s", s).setParameter("e", e).getSingleResult();
        return toBd(r);
    }

    private long countSellerOrdersBetween(Long sid, LocalDateTime s, LocalDateTime e) {
        return (Long) em.createQuery("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi JOIN oi.product p WHERE p.seller.id = :sid AND oi.order.createdAt >= :s AND oi.order.createdAt < :e")
                .setParameter("sid", sid).setParameter("s", s).setParameter("e", e).getSingleResult();
    }

    private BigDecimal toBd(Object o) { return o instanceof BigDecimal ? (BigDecimal) o : BigDecimal.valueOf(((Number) o).doubleValue()); }

    private double calcChange(BigDecimal prev, BigDecimal cur) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return cur != null && cur.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        return cur.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
