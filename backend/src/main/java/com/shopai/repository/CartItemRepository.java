package com.shopai.repository;

import com.shopai.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(Long cartId, Long productId, Long variantId);
}
