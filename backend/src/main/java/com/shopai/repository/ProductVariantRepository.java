package com.shopai.repository;

import com.shopai.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = pv.stockQuantity - :qty WHERE pv.id = :id AND pv.stockQuantity >= :qty")
    int decrementStock(Long id, int qty);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = pv.stockQuantity + :qty WHERE pv.id = :id")
    void incrementStock(Long id, int qty);
}
