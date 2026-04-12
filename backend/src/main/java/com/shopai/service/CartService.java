package com.shopai.service;

import com.shopai.dto.request.CartRequests.*;
import com.shopai.dto.response.CartResponse;
import com.shopai.entity.*;
import com.shopai.exception.BadRequestException;
import com.shopai.exception.ResourceNotFoundException;
import com.shopai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final AuditLogService auditLogService;

    // In-memory coupon state per cart (production'da Redis/DB'de saklanmalı)
    private final Map<Long, String> cartCoupons = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> cartDiscounts = new ConcurrentHashMap<>();

    // ─── Sepeti Getir ────────────────────────────────────────────────────────
    @Transactional // readOnly=true OLMAMALI: getOrCreateCart() yeni cart oluşturabilir (INSERT)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartResponse.from(cart, cartCoupons.get(cart.getId()), cartDiscounts.get(cart.getId()));
    }

    // ─── Sepete Ürün Ekle ────────────────────────────────────────────────────
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest req, jakarta.servlet.http.HttpServletRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(req.getProductId())
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün", req.getProductId()));

        // Stok kontrolü
        if (product.getStockQuantity() < req.getQuantity()) {
            throw new BadRequestException("Stok yetersiz. Mevcut: " + product.getStockQuantity());
        }

        // Aynı ürün + varyant zaten sepette mi?
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductIdAndVariantId(
                cart.getId(), req.getProductId(), req.getVariantId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int oldQty = item.getQuantity();
            int newQty = oldQty + req.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new BadRequestException("Sepetteki miktar stok limitini aşıyor");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
            
            auditLogService.logWithMap(userId, "CART_ITEM_UPDATE_QTY", "CartItem", item.getId(), 
                    Map.of("quantity", oldQty), Map.of("quantity", newQty), null, request.getHeader("User-Agent"));
        } else {
            // Fiyat snapshot — sepete eklendiği andaki fiyat kaydedilir
            BigDecimal priceAtAdd = product.getEffectivePrice();
            if (req.getVariantId() != null) {
                priceAtAdd = priceAtAdd.add(
                        product.getVariants().stream()
                                .filter(v -> v.getId().equals(req.getVariantId()))
                                .findFirst()
                                .map(ProductVariant::getPriceModifier)
                                .orElse(BigDecimal.ZERO));
            }

            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(req.getQuantity())
                    .priceAtAdd(priceAtAdd)
                    .build();

            if (req.getVariantId() != null) {
                product.getVariants().stream()
                        .filter(v -> v.getId().equals(req.getVariantId()))
                        .findFirst()
                        .ifPresent(item::setVariant);
            }

            cartItemRepository.save(item);
            cart.getItems().add(item); // İlişkiyi manuel senkronize et (L1 lag fix)
            
            auditLogService.logEntityAction(userId, "CART_ITEM_ADD", null, item, "CartItem", item.getId(), request);
        }

        return getCart(userId);
    }

    // ─── Ürün Miktarını Güncelle ─────────────────────────────────────────────
    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, UpdateQuantityRequest req, jakarta.servlet.http.HttpServletRequest request) {
        // Ownership check — kullanıcının kendi sepet item'ı mı?
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sepet ürünü bulunamadı"));

        if (req.getQuantity() > item.getProduct().getStockQuantity()) {
            throw new BadRequestException("Stok yetersiz");
        }
        
        int oldQty = item.getQuantity();
        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);
        
        auditLogService.logWithMap(userId, "CART_ITEM_UPDATE_QTY", "CartItem", itemId, 
                Map.of("quantity", oldQty), Map.of("quantity", req.getQuantity()), null, request.getHeader("User-Agent"));
                
        return getCart(userId);
    }

    // ─── Ürünü Sepetten Çıkar ────────────────────────────────────────────────
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId, jakarta.servlet.http.HttpServletRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sepet ürünü bulunamadı"));
        Cart cart = item.getCart();
        
        auditLogService.logEntityAction(userId, "CART_ITEM_REMOVE", item, null, "CartItem", itemId, request);
        
        cart.getItems().remove(item); // İlişkiyi manuel senkronize et
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    // ─── Sepeti Temizle ──────────────────────────────────────────────────────
    @Transactional
    public void clearCart(Long userId, jakarta.servlet.http.HttpServletRequest request) {
        Cart cart = getOrCreateCart(userId);
        
        auditLogService.logWithMap(userId, "CART_CLEAR", "Cart", cart.getId(), 
                Map.of("itemCount", cart.getItems().size()), null, null, request.getHeader("User-Agent"));
                
        cart.getItems().clear();
        cartRepository.save(cart);
        cartCoupons.remove(cart.getId());
        cartDiscounts.remove(cart.getId());
    }

    // ─── Kupon Uygula ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse applyCoupon(Long userId, ApplyCouponRequest req, jakarta.servlet.http.HttpServletRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Mevcut subtotal'ı hesapla
        BigDecimal subtotal = cart.getItems().stream()
                .map(i -> i.getPriceAtAdd().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(req.getCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Geçersiz kupon kodu"));

        if (!coupon.isValid(subtotal)) {
            throw new BadRequestException("Bu kupon geçerli değil veya minimum tutar sağlanmıyor");
        }

        BigDecimal discount = coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE
                ? subtotal.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100)))
                : coupon.getDiscountValue();

        discount = discount.min(subtotal); // indirim subtotal'ı geçemesin

        String oldCoupon = cartCoupons.get(cart.getId());
        cartCoupons.put(cart.getId(), coupon.getCode());
        cartDiscounts.put(cart.getId(), discount);

        auditLogService.logWithMap(userId, "CART_COUPON_APPLY", "Cart", cart.getId(), 
                oldCoupon != null ? Map.of("coupon", oldCoupon) : null, 
                Map.of("coupon", coupon.getCode()), null, request.getHeader("User-Agent"));

        return getCart(userId);
    }

    // ─── Kuponu Kaldır ───────────────────────────────────────────────────────
    @Transactional
    public CartResponse removeCoupon(Long userId, jakarta.servlet.http.HttpServletRequest request) {
        Cart cart = getOrCreateCart(userId);
        String oldCoupon = cartCoupons.remove(cart.getId());
        cartDiscounts.remove(cart.getId());
        
        auditLogService.logWithMap(userId, "CART_COUPON_REMOVE", "Cart", cart.getId(), 
                oldCoupon != null ? Map.of("coupon", oldCoupon) : null, null, null, request.getHeader("User-Agent"));
                
        return getCart(userId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    public String getAppliedCoupon(Long cartId) {
        return cartCoupons.get(cartId);
    }

    public BigDecimal getDiscount(Long cartId) {
        return cartDiscounts.getOrDefault(cartId, BigDecimal.ZERO);
    }
}
