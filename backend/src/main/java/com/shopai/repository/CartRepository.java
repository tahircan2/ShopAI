package com.shopai.repository;

import com.shopai.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.variant WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(Long userId);

    Optional<Cart> findByUserId(Long userId);
}
