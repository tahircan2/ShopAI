package com.shopai.service;

import com.shopai.dto.request.AgentRequests.QuickCheckoutExecuteRequest;
import com.shopai.dto.request.AgentRequests.QuickCheckoutValidateRequest;
import com.shopai.dto.response.AgentResponses.*;
import com.shopai.entity.*;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ForbiddenException;
import com.shopai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuickCheckoutService {

    private final AgentSecurityService securityService;
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CouponRepository couponRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("29.90");

    /**
     * Checkout öncesi doğrulama — stok, adres, kupon kontrolü.
     * Bu metod AI pre_validation_agent tarafından çağrılır.
     */
    @Transactional
    public QuickCheckoutValidationResponse validateCheckout(User user, QuickCheckoutValidateRequest request) {
        log.info("Validating quick checkout for user id: {}", user.getId());

        List<ValidationIssue> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Sepet kontrolü
        Cart cart = cartService.getOrCreateCart(user.getId());
        if (cart.getItems().isEmpty()) {
            issues.add(ValidationIssue.builder()
                    .field("cart").message("Sepetiniz boş").severity("ERROR").build());
        }

        // 2. Stok kontrolü — her ürün için mevcut stok yeterli mi?
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.getIsActive()) {
                issues.add(ValidationIssue.builder()
                        .field("product").message(product.getName() + " artık satışta değil").severity("ERROR").build());
                continue;
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                issues.add(ValidationIssue.builder()
                        .field("stock")
                        .message(product.getName() + " için yeterli stok yok (istenen: " + item.getQuantity() + ", mevcut: " + product.getStockQuantity() + ")")
                        .severity("ERROR").build());
            }
            // Varyant stok kontrolü
            if (item.getVariant() != null && item.getVariant().getStockQuantity() < item.getQuantity()) {
                issues.add(ValidationIssue.builder()
                        .field("variant_stock")
                        .message(product.getName() + " varyantı için stok yetersiz")
                        .severity("ERROR").build());
            }
        }

        // 3. Adres doğrulaması
        if (request.getShippingAddressId() != null) {
            addressRepository.findByIdAndUserId(request.getShippingAddressId(), user.getId())
                    .orElseGet(() -> {
                        issues.add(ValidationIssue.builder()
                                .field("address").message("Seçili teslimat adresi bulunamadı veya size ait değil").severity("ERROR").build());
                        return null;
                    });
        } else {
            issues.add(ValidationIssue.builder()
                    .field("address").message("Teslimat adresi belirtilmemiş").severity("ERROR").build());
        }

        // 4. Kupon geçerliliği
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            couponRepository.findByCodeAndIsActiveTrue(request.getCouponCode().toUpperCase())
                    .ifPresentOrElse(
                            coupon -> {
                                BigDecimal subtotal = calculateSubtotal(cart);
                                if (!coupon.isValid(subtotal)) {
                                    warnings.add("Kupon '" + request.getCouponCode() + "' bu sepet için geçerli değil");
                                }
                            },
                            () -> warnings.add("Kupon kodu '" + request.getCouponCode() + "' bulunamadı")
                    );
        }

        // 5. Sepet özeti oluştur
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = cartService.getDiscount(cart.getId());
        BigDecimal afterDiscount = subtotal.subtract(discount).max(BigDecimal.ZERO);
        boolean freeShipping = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
        BigDecimal shippingCost = freeShipping ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal tax = afterDiscount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        CartSummary cartSummary = CartSummary.builder()
                .itemCount(cart.getItems().size())
                .subtotal(subtotal)
                .taxAmount(tax)
                .shippingCost(shippingCost)
                .discountAmount(discount)
                .total(afterDiscount.add(shippingCost).add(tax))
                .appliedCoupon(cartService.getAppliedCoupon(cart.getId()))
                .build();

        return QuickCheckoutValidationResponse.builder()
                .valid(issues.isEmpty())
                .issues(issues)
                .warnings(warnings)
                .cartSummary(cartSummary)
                .build();
    }

    /**
     * Checkout yürütme — onay token doğrulaması sonrası sipariş oluşturma.
     * Bu metod multi_step_executor tarafından çağrılır.
     */
    @Transactional
    public QuickCheckoutResponse executeCheckout(User user, QuickCheckoutExecuteRequest request, HttpServletRequest httpRequest) {
        log.info("Executing quick checkout for user id: {}", user.getId());

        // 1. Onay token doğrulaması (Kriptografik bütünlük kontrolü)
        boolean isValid = securityService.verifyPlanIntegrity(request.getApprovalToken(), request.getPlanData());
        if (!isValid) {
            throw new BadRequestException("İşlem planı değiştirilmiş veya onay token'ı geçersiz.");
        }

        // 2. Plan içeriği ile request eşleşiyor mu? (Alan bazlı kontrol)
        try {
            JsonNode plan = objectMapper.readTree(request.getPlanData());
            
            // Adres ID kontrolü
            Long planAddressId = plan.has("shippingAddressId") && !plan.get("shippingAddressId").isNull() 
                    ? plan.get("shippingAddressId").asLong() : null;
            if (planAddressId != null && !planAddressId.equals(request.getShippingAddressId())) {
                log.warn("PLAN_MISMATCH: Address mismatch. Plan: {}, Req: {}", planAddressId, request.getShippingAddressId());
                throw new BadRequestException("Seçili adres onaylanan plan ile uyuşmuyor.");
            }

            // Kupon kontrolü (Boş string vs null durumlarını normalize et)
            String planCoupon = plan.has("couponCode") && !plan.get("couponCode").isNull() 
                    ? plan.get("couponCode").asText() : "";
            String reqCoupon = request.getCouponCode() != null ? request.getCouponCode() : "";
            if (!planCoupon.equals(reqCoupon)) {
                log.warn("PLAN_MISMATCH: Coupon mismatch. Plan: {}, Req: {}", planCoupon, reqCoupon);
                throw new BadRequestException("Kupon kodu onaylanan plan ile uyuşmuyor.");
            }

            // Ödeme yöntemi kontrolü
            String planPayment = plan.has("paymentMethod") && !plan.get("paymentMethod").isNull() 
                    ? plan.get("paymentMethod").asText() : "KAPIDA_ODEME";
            if (!planPayment.equals(request.getPaymentMethod())) {
                log.warn("PLAN_MISMATCH: Payment method mismatch. Plan: {}, Req: {}", planPayment, request.getPaymentMethod());
                throw new BadRequestException("Ödeme yöntemi onaylanan plan ile uyuşmuyor.");
            }
        } catch (Exception e) {
            log.error("Plan data parsing error", e);
            if (e instanceof BadRequestException) throw (BadRequestException)e;
            throw new BadRequestException("İşlem planı verisi okunamadı.");
        }

        // 3. Adres doğrula (IDOR koruması — kullanıcının kendi adresi mi?)
        Address address = addressRepository.findByIdAndUserId(request.getShippingAddressId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Geçersiz teslimat adresi veya yetkisiz işlem"));

        // 3. Sepet kontrolü
        Cart cart = cartService.getOrCreateCart(user.getId());
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Sepetiniz boş, sipariş oluşturulamaz.");
        }

        // 4. Stok düşür ve sipariş kalemleri oluştur
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Atomik stok düşürme
            int updated = productRepository.decrementStock(product.getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new BadRequestException(product.getName() + " için stok yetersiz.");
            }

            // Varyant stok düşürme
            if (cartItem.getVariant() != null) {
                int variantUpdated = productVariantRepository.decrementStock(
                        cartItem.getVariant().getId(), cartItem.getQuantity());
                if (variantUpdated == 0) {
                    productRepository.incrementStock(product.getId(), cartItem.getQuantity());
                    throw new BadRequestException(product.getName() + " varyantı için stok yetersiz.");
                }
            }

            BigDecimal unitPrice = cartItem.getPriceAtAdd();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getId() : null)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        // 5. Kupon uygulama
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = cartService.getAppliedCoupon(cart.getId());
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCouponCode = request.getCouponCode();
        }

        if (appliedCouponCode != null) {
            couponRepository.findByCodeAndIsActiveTrue(appliedCouponCode).ifPresent(coupon -> {
                couponRepository.incrementUsedCount(coupon.getCode());
            });
            discountAmount = cartService.getDiscount(cart.getId());
        }

        // 6. Hesaplamalar
        BigDecimal afterDiscount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        boolean freeShipping = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
        BigDecimal shippingCost = freeShipping ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal taxAmount = afterDiscount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = afterDiscount.add(shippingCost).add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // 7. Sipariş oluştur
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount)
                .shippingCost(shippingCost)
                .discountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(totalAmount)
                .couponCode(appliedCouponCode)
                .shippingAddress(address)
                .notes(request.getNotes())
                .paymentMethod(request.getPaymentMethod())
                .isAiCreated(true)
                .aiAgentType("checkout_orchestration")
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(orderItems);

        Order saved = orderRepository.save(order);

        // 8. Sepeti temizle
        cartService.clearCart(user.getId(), httpRequest);

        // 9. Token'ı kullanıldı olarak işaretle
        securityService.markTokenAsUsed(request.getApprovalToken(), saved.getId());

        // 10. Bildirimler
        notificationService.sendOrderStatusNotification(user.getId(), saved.getOrderNumber(), "PENDING", saved.getId());

        // 11. Audit log
        auditLogService.logWithMap(user.getId(), "AI_ORDER_CREATED", "Order", saved.getId(),
                null, java.util.Map.of(
                        "orderNumber", saved.getOrderNumber(),
                        "totalAmount", totalAmount.toPlainString(),
                        "aiAgent", "checkout_orchestration"
                ), null, httpRequest != null ? httpRequest.getHeader("User-Agent") : null);

        log.info("AI Quick Checkout order created: {} for userId: {}", saved.getOrderNumber(), user.getId());

        return QuickCheckoutResponse.builder()
                .success(true)
                .orderNumber(saved.getOrderNumber())
                .message("Sipariş başarıyla oluşturuldu!")
                .totalAmount(totalAmount)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(i -> i.getPriceAtAdd().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        String candidate = "ORD-" + date + "-" + random;
        while (orderRepository.existsByOrderNumber(candidate)) {
            random = String.format("%04d", new Random().nextInt(10000));
            candidate = "ORD-" + date + "-" + random;
        }
        return candidate;
    }
}
