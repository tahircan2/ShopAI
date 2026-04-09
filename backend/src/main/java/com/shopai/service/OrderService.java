package com.shopai.service;

import com.shopai.dto.request.OrderRequests.*;
import com.shopai.dto.response.OrderResponses.*;
import com.shopai.entity.*;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.exception.StockException;
import com.shopai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("29.90");

    // ─── Sipariş Oluştur ─────────────────────────────────────────────────────
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", userId));

        // Teslimat adresi — ownership check
        Address shippingAddress = addressRepository.findByIdAndUserId(req.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı veya erişim yetkiniz yok"));

        // Sepet
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Sepetiniz boş");
        }

        // Stok kontrolü ve fiyat snapshot
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Stok düşür — atomik sorgu, race condition'a karşı
            int updated = productRepository.decrementStock(product.getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new StockException(product.getName(), cartItem.getQuantity(), product.getStockQuantity());
            }

            // Sipariş anındaki fiyat snapshot (price_at_add)
            BigDecimal unitPrice = cartItem.getPriceAtAdd();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getId() : null)
                    .productName(product.getName())   // anlık snapshot
                    .productSku(product.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        // İndirim
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = cartService.getAppliedCoupon(cart.getId());

        if (appliedCouponCode != null) {
            couponRepository.findByCodeAndIsActiveTrue(appliedCouponCode).ifPresent(coupon -> {
                // Kupon kullanım sayısını arttır
                couponRepository.incrementUsedCount(coupon.getCode());
            });
            discountAmount = cartService.getDiscount(cart.getId());
        }

        BigDecimal afterDiscount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        // Kargo
        boolean freeShipping = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
        BigDecimal shippingCost = freeShipping ? BigDecimal.ZERO : SHIPPING_COST;

        // KDV %18
        BigDecimal taxAmount = afterDiscount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = afterDiscount.add(shippingCost).add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // Sipariş oluştur
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount)
                .shippingCost(shippingCost)
                .discountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(totalAmount)
                .couponCode(appliedCouponCode)
                .shippingAddress(shippingAddress)
                .notes(req.getNotes())
                .paymentMethod(req.getPaymentMethod())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(orderItems);

        Order saved = orderRepository.save(order);

        // Sepeti temizle
        cartService.clearCart(userId);

        // Bildirim + e-posta (async)
        notificationService.sendOrderStatusNotification(userId, saved.getOrderNumber(), "PENDING", saved.getId());
        emailService.sendOrderConfirmation(user.getEmail(), saved.getOrderNumber(),
                totalAmount.toPlainString());
        auditLogService.log(userId, "ORDER_CREATED", "Order", saved.getId(), ip, null);

        log.info("Order created: {} for userId: {}", saved.getOrderNumber(), userId);
        return OrderResponse.from(saved);
    }

    // ─── Siparişleri Getir (kullanıcı) ───────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getUserOrders(Long userId, int page, int size) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 20)))
                .map(OrderSummaryResponse::from);
    }

    // ─── Sipariş Detayı ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long userId, String orderNumber) {
        // ownership check — kullanıcı yalnızca kendi siparişini görebilir
        Order order = orderRepository.findByOrderNumberAndUserIdWithItems(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderNumber));
        return OrderResponse.from(order);
    }

    // ─── Sipariş İptal ───────────────────────────────────────────────────────
    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderNumber, String ip) {
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderNumber));

        if (!order.isCancellable()) {
            throw new BadRequestException("Bu sipariş iptal edilemez. Mevcut durum: " + order.getStatus());
        }

        // Stokları geri yükle
        order.getItems().forEach(item -> {
            if (item.getProduct() != null) {
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
        });

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        notificationService.sendOrderStatusNotification(userId, orderNumber, "CANCELLED", saved.getId());
        auditLogService.log(userId, "ORDER_CANCELLED", "Order", saved.getId(), ip, null);

        return OrderResponse.from(saved);
    }

    // ─── Admin: Tüm Siparişler ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(int page, int size) {
        return orderRepository.findAll(
                PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(OrderSummaryResponse::from);
    }

    // ─── Admin: Sipariş Durumu Güncelle ─────────────────────────────────────
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş", orderId));

        Order.OrderStatus newStatus;
        try {
            newStatus = Order.OrderStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Geçersiz sipariş durumu: " + req.getStatus());
        }

        order.setStatus(newStatus);

        if (newStatus == Order.OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (newStatus == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);

        notificationService.sendOrderStatusNotification(
                order.getUser().getId(), order.getOrderNumber(), newStatus.name(), saved.getId());
        emailService.sendOrderStatusUpdate(
                order.getUser().getEmail(), order.getOrderNumber(), newStatus.name());

        return OrderResponse.from(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        String candidate = "ORD-" + date + "-" + random;
        // Benzersizlik garantisi
        while (orderRepository.existsByOrderNumber(candidate)) {
            random = String.format("%04d", new Random().nextInt(10000));
            candidate = "ORD-" + date + "-" + random;
        }
        return candidate;
    }
}
