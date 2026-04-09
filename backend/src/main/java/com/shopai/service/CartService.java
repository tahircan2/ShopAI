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
    public CartResponse addToCart(Long userId, AddToCartRequest req) {
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
            int newQty = item.getQuantity() + req.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new BadRequestException("Sepetteki miktar stok limitini aşıyor");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
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
        }

        return getCart(userId);
    }

    // ─── Ürün Miktarını Güncelle ─────────────────────────────────────────────
    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, UpdateQuantityRequest req) {
        // Ownership check — kullanıcının kendi sepet item'ı mı?
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sepet ürünü bulunamadı"));

        if (req.getQuantity() > item.getProduct().getStockQuantity()) {
            throw new BadRequestException("Stok yetersiz");
        }
        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);
        return getCart(userId);
    }

    // ─── Ürünü Sepetten Çıkar ────────────────────────────────────────────────
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sepet ürünü bulunamadı"));
        Cart cart = item.getCart();
        cart.getItems().remove(item); // İlişkiyi manuel senkronize et
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    // ─── Sepeti Temizle ──────────────────────────────────────────────────────
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        cartCoupons.remove(cart.getId());
        cartDiscounts.remove(cart.getId());
    }

    // ─── Kupon Uygula ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse applyCoupon(Long userId, ApplyCouponRequest req) {
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

        cartCoupons.put(cart.getId(), coupon.getCode());
        cartDiscounts.put(cart.getId(), discount);

        return getCart(userId);
    }

    // ─── Kuponu Kaldır ───────────────────────────────────────────────────────
    @Transactional
    public CartResponse removeCoupon(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartCoupons.remove(cart.getId());
        cartDiscounts.remove(cart.getId());
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
