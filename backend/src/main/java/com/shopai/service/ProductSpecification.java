package com.shopai.service;

import com.shopai.dto.request.ProductRequests.ProductFilterRequest;
import com.shopai.entity.Category;
import com.shopai.entity.Product;
import com.shopai.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> withFilter(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sadece aktif ürünler
            predicates.add(cb.isTrue(root.get("isActive")));

            // Serbest metin arama (q parametresi)
            if (filter.getQ() != null && !filter.getQ().isBlank()) {
                String kw = "%" + filter.getQ().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), kw),
                        cb.like(cb.lower(root.get("description")), kw),
                        cb.like(cb.lower(root.get("brand")), kw)
                ));
            }

            // Kategori filtresi — Recursive destekli
            if (filter.getCategoryIds() != null && !filter.getCategoryIds().isEmpty()) {
                // Direct ID search is safer and more efficient
                predicates.add(root.get("category").get("id").in(filter.getCategoryIds()));
            } else {
                boolean hasCatSlug = filter.getCategorySlug() != null && !filter.getCategorySlug().isBlank();
                boolean hasCatId   = filter.getCategoryId() != null;
                
                if (hasCatSlug || hasCatId) {
                    Join<Product, Category> catJoin = root.join("category", JoinType.INNER);
                    if (hasCatSlug) {
                        predicates.add(cb.equal(cb.lower(catJoin.get("slug")), filter.getCategorySlug().trim().toLowerCase()));
                    }
                    if (hasCatId) {
                        predicates.add(cb.equal(catJoin.get("id"), filter.getCategoryId()));
                    }
                }
            }

            // Fiyat aralığı (effectivePrice = discountedPrice varsa o, yoksa price)
            if (filter.getMinPrice() != null) {
                Predicate discountedMin = cb.and(
                        cb.isNotNull(root.get("discountedPrice")),
                        cb.greaterThanOrEqualTo(root.get("discountedPrice"), filter.getMinPrice())
                );
                Predicate normalMin = cb.and(
                        cb.isNull(root.get("discountedPrice")),
                        cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice())
                );
                predicates.add(cb.or(discountedMin, normalMin));
            }

            if (filter.getMaxPrice() != null) {
                Predicate discountedMax = cb.and(
                        cb.isNotNull(root.get("discountedPrice")),
                        cb.lessThanOrEqualTo(root.get("discountedPrice"), filter.getMaxPrice())
                );
                Predicate normalMax = cb.and(
                        cb.isNull(root.get("discountedPrice")),
                        cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice())
                );
                predicates.add(cb.or(discountedMax, normalMax));
            }

            // Marka filtresi
            if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("brand")),
                        "%" + filter.getBrand().toLowerCase() + "%"));
            }

            // Minimum puan
            if (filter.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("ratingAvg"),
                        new java.math.BigDecimal(filter.getMinRating().toString())
                ));
            }

            // Stok filtresi
            if (Boolean.TRUE.equals(filter.getInStock())) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }

            // Renk ve beden filtresi — ProductVariant join
            if ((filter.getColors() != null && !filter.getColors().isEmpty()) ||
                (filter.getSizes() != null && !filter.getSizes().isEmpty())) {

                Subquery<Long> variantSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = variantSubquery.from(ProductVariant.class);
                variantSubquery.select(variantRoot.get("product").get("id"));

                List<Predicate> variantPredicates = new ArrayList<>();
                variantPredicates.add(cb.equal(variantRoot.get("product").get("id"), root.get("id")));

                if (filter.getColors() != null && !filter.getColors().isEmpty()) {
                    variantPredicates.add(variantRoot.get("color").in(filter.getColors()));
                }
                if (filter.getSizes() != null && !filter.getSizes().isEmpty()) {
                    variantPredicates.add(variantRoot.get("size").in(filter.getSizes()));
                }
                variantSubquery.where(cb.and(variantPredicates.toArray(new Predicate[0])));
                predicates.add(cb.exists(variantSubquery));
            }

            // Sıralama
            String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
            boolean asc = "asc".equalsIgnoreCase(filter.getSortDir());

            Expression<?> sortExpr = switch (sortBy) {
                case "price" -> root.get("price");
                case "rating" -> root.get("ratingAvg");
                case "ratingCount" -> root.get("ratingCount");
                default -> root.get("createdAt");
            };

            query.orderBy(asc ? cb.asc(sortExpr) : cb.desc(sortExpr));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
