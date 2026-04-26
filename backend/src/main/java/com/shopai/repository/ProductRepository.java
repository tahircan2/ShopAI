package com.shopai.repository;

import com.shopai.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndIsActiveTrue(String slug);
    Optional<Product> findBySlug(String slug);
    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);

    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    long countBySellerId(Long sellerId);

    @Query("SELECT AVG(p.ratingAvg) FROM Product p WHERE p.seller.id = :sellerId AND p.isActive = true")
    Double avgRatingForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> searchByKeyword(String q, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(Long id, int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    void incrementStock(Long id, int qty);

    @Modifying
    @Query("UPDATE Product p SET p.ratingAvg = :avg, p.ratingCount = :count WHERE p.id = :id")
    void updateRating(Long id, BigDecimal avg, int count);
}
