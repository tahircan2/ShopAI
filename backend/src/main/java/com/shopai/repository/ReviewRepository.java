package com.shopai.repository;

import com.shopai.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndIsApprovedTrue(Long productId, Pageable pageable);
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    Optional<Review> findByIdAndUserId(Long id, Long userId);
    Page<Review> findByUserId(Long userId, Pageable pageable);
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double findAvgRatingByProductId(Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Long countByProductId(Long productId);
}
