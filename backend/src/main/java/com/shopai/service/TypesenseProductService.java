package com.shopai.service;

import com.shopai.entity.Product;
import com.shopai.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.api.FieldTypes;
import org.typesense.model.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Typesense arama motoru servisi.
 * <p>
 * MySQL ana veri kaynağı (source of truth) olmaya devam eder.
 * Bu servis yalnızca arama indeksi olarak Typesense ile iletişim kurar.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "typesense.enabled", havingValue = "true", matchIfMissing = false)
public class TypesenseProductService {

    private static final String COLLECTION_NAME = "products";

    private final Client typesenseClient;

    // ─── Koleksiyon Yönetimi ─────────────────────────────────────────────────

    /**
     * Typesense'de 'products' koleksiyonunu oluşturur.
     * Zaten varsa hata vermez, sessizce atlar.
     */
    public void ensureCollection() {
        try {
            typesenseClient.collections(COLLECTION_NAME).retrieve();
            log.info("Typesense '{}' koleksiyonu zaten mevcut.", COLLECTION_NAME);
        } catch (Exception e) {
            // Koleksiyon yok — oluştur
            try {
                createCollection();
                log.info("Typesense '{}' koleksiyonu başarıyla oluşturuldu.", COLLECTION_NAME);
            } catch (Exception ex) {
                log.error("Typesense koleksiyon oluşturma hatası: {}", ex.getMessage(), ex);
            }
        }
    }

    private void createCollection() throws Exception {
        List<Field> fields = new ArrayList<>();

        // Aranabilir metin alanları
        fields.add(new Field().name("name").type(FieldTypes.STRING));
        fields.add(new Field().name("slug").type(FieldTypes.STRING).index(false).optional(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).optional(true));

        // Sayısal alanlar (filtreleme + sıralama)
        fields.add(new Field().name("price").type(FieldTypes.FLOAT));
        fields.add(new Field().name("discountedPrice").type(FieldTypes.FLOAT).optional(true));
        fields.add(new Field().name("effectivePrice").type(FieldTypes.FLOAT).facet(true));
        fields.add(new Field().name("stockQuantity").type(FieldTypes.INT32));

        // Marka — aranabilir + facet
        fields.add(new Field().name("brand").type(FieldTypes.STRING).facet(true).optional(true));

        // Kategori bilgileri — facet
        fields.add(new Field().name("categoryName").type(FieldTypes.STRING).facet(true).optional(true));
        fields.add(new Field().name("categorySlug").type(FieldTypes.STRING).facet(true).optional(true));
        fields.add(new Field().name("categoryId").type(FieldTypes.INT64).facet(true).optional(true));

        // Etiketler — facet
        fields.add(new Field().name("tags").type(FieldTypes.STRING_ARRAY).facet(true).optional(true));

        // Puan
        fields.add(new Field().name("ratingAvg").type(FieldTypes.FLOAT).facet(true));
        fields.add(new Field().name("ratingCount").type(FieldTypes.INT32));

        // Boolean alanlar
        fields.add(new Field().name("isFeatured").type(FieldTypes.BOOL).facet(true));
        fields.add(new Field().name("isActive").type(FieldTypes.BOOL).facet(true));

        // Görsel
        fields.add(new Field().name("primaryImageUrl").type(FieldTypes.STRING).index(false).optional(true));

        // Satıcı
        fields.add(new Field().name("sellerId").type(FieldTypes.INT64).facet(true).optional(true));
        fields.add(new Field().name("sellerName").type(FieldTypes.STRING).optional(true));

        // Zaman damgası (sıralama için unix timestamp)
        fields.add(new Field().name("createdAt").type(FieldTypes.INT64));

        CollectionSchema schema = new CollectionSchema();
        schema.name(COLLECTION_NAME)
              .fields(fields)
              .defaultSortingField("createdAt");

        typesenseClient.collections().create(schema);
    }

    // ─── Ürün İndeksleme (Upsert) ──────────────────────────────────────────

    /**
     * Ürünü Typesense'e yazar. Zaten varsa günceller (upsert).
     */
    public void indexProduct(Product product) {
        try {
            Map<String, Object> doc = toDocument(product);
            typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .upsert(doc);
            log.debug("Typesense index: ürün #{} indexlendi", product.getId());
        } catch (Exception e) {
            log.error("Typesense index hatası (ürün #{}): {}", product.getId(), e.getMessage(), e);
        }
    }

    /**
     * Birden çok ürünü toplu olarak indexler.
     */
    public void bulkIndex(List<Product> products) {
        if (products.isEmpty()) return;

        try {
            List<Map<String, Object>> docs = products.stream()
                    .map(this::toDocument)
                    .collect(Collectors.toList());

            // JSONL formatında import
            StringBuilder jsonl = new StringBuilder();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (Map<String, Object> doc : docs) {
                jsonl.append(mapper.writeValueAsString(doc)).append("\n");
            }

            ImportDocumentsParameters params = new ImportDocumentsParameters();
            params.action(IndexAction.UPSERT);

            typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .import_(jsonl.toString(), params);

            log.info("Typesense toplu index: {} ürün başarıyla indexlendi", products.size());
        } catch (Exception e) {
            log.error("Typesense toplu index hatası ({} ürün): {}", products.size(), e.getMessage(), e);
            throw new RuntimeException("Typesense toplu indexleme başarısız oldu", e);
        }
    }

    // ─── Ürün Silme ─────────────────────────────────────────────────────────

    /**
     * Ürünü Typesense indeksinden siler.
     */
    public void removeProduct(Long productId) {
        try {
            typesenseClient.collections(COLLECTION_NAME)
                    .documents(String.valueOf(productId))
                    .delete();
            log.debug("Typesense'den silindi: ürün #{}", productId);
        } catch (Exception e) {
            log.error("Typesense silme hatası (ürün #{}): {}", productId, e.getMessage(), e);
        }
    }

    // ─── Arama ──────────────────────────────────────────────────────────────

    /**
     * Typo-tolerant metin araması. Sonuç olarak eşleşen ürün ID'lerini döner.
     *
     * @param q    Arama terimi
     * @param page Sayfa numarası (0-indexed)
     * @param size Sayfa boyutu
     * @return Eşleşen ürün ID listesi ve toplam sonuç sayısı
     */
    public TypesenseSearchResult search(String q, int page, int size) {
        try {
            SearchParameters searchParams = new SearchParameters()
                    .q(q)
                    .queryBy("name,description,brand,categoryName")
                    .filterBy("isActive:true")
                    .sortBy("_text_match:desc,ratingAvg:desc")
                    .numTypos("2")
                    .perPage(size)
                    .page(page + 1); // Typesense 1-indexed

            SearchResult result = typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .search(searchParams);

            List<Long> ids = extractIds(result);
            int totalFound = result.getFound() != null ? result.getFound().intValue() : 0;

            log.debug("Typesense arama: q='{}', bulunan={}", q, totalFound);
            return new TypesenseSearchResult(ids, totalFound);
        } catch (Exception e) {
            log.error("Typesense arama hatası: {}", e.getMessage(), e);
            return new TypesenseSearchResult(List.of(), 0);
        }
    }

    /**
     * Filtrelenmiş arama — chatbot ve frontend filtresi için.
     */
    public TypesenseSearchResult filter(
            String q,
            String categorySlug,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String brand,
            Double minRating,
            Boolean inStock,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        try {
            // Query text
            String queryText = (q != null && !q.isBlank()) ? q : "*";

            // Filter oluştur
            List<String> filters = new ArrayList<>();
            filters.add("isActive:true");

            if (categorySlug != null && !categorySlug.isBlank()) {
                filters.add("categorySlug:=" + categorySlug);
            }
            if (categoryId != null) {
                filters.add("categoryId:=" + categoryId);
            }
            if (minPrice != null) {
                filters.add("effectivePrice:>=" + minPrice.doubleValue());
            }
            if (maxPrice != null) {
                filters.add("effectivePrice:<=" + maxPrice.doubleValue());
            }
            if (brand != null && !brand.isBlank()) {
                filters.add("brand:=" + brand);
            }
            if (minRating != null) {
                filters.add("ratingAvg:>=" + minRating);
            }
            if (Boolean.TRUE.equals(inStock)) {
                filters.add("stockQuantity:>0");
            }

            String filterBy = String.join(" && ", filters);

            // Sort oluştur
            String sort = buildSortClause(sortBy, sortDir);

            SearchParameters searchParams = new SearchParameters()
                    .q(queryText)
                    .queryBy("name,description,brand")
                    .filterBy(filterBy)
                    .sortBy(sort)
                    .perPage(size)
                    .page(page + 1);

            SearchResult result = typesenseClient.collections(COLLECTION_NAME)
                    .documents()
                    .search(searchParams);

            List<Long> ids = extractIds(result);
            int totalFound = result.getFound() != null ? result.getFound().intValue() : 0;

            log.debug("Typesense filtreli arama: q='{}', filter='{}', bulunan={}", queryText, filterBy, totalFound);
            return new TypesenseSearchResult(ids, totalFound);
        } catch (Exception e) {
            log.error("Typesense filtreli arama hatası: {}", e.getMessage(), e);
            return new TypesenseSearchResult(List.of(), 0);
        }
    }

    // ─── Yardımcı Metotlar ──────────────────────────────────────────────────

    private Map<String, Object> toDocument(Product product) {
        Map<String, Object> doc = new HashMap<>();

        doc.put("id", String.valueOf(product.getId()));
        doc.put("name", product.getName() != null ? product.getName() : "");
        doc.put("slug", product.getSlug());
        doc.put("description", product.getDescription() != null ? product.getDescription() : "");

        doc.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        doc.put("discountedPrice", product.getDiscountedPrice() != null
                ? product.getDiscountedPrice().doubleValue() : 0.0);
        doc.put("effectivePrice", product.getEffectivePrice() != null
                ? product.getEffectivePrice().doubleValue() : 0.0);
        doc.put("stockQuantity", product.getStockQuantity() != null ? product.getStockQuantity() : 0);

        doc.put("brand", product.getBrand() != null ? product.getBrand() : "");

        // Kategori
        if (product.getCategory() != null) {
            String fullCategoryName = product.getCategory().getName();
            if (product.getCategory().getParent() != null) {
                fullCategoryName += " " + product.getCategory().getParent().getName();
            }
            doc.put("categoryName", fullCategoryName);
            doc.put("categorySlug", product.getCategory().getSlug());
            doc.put("categoryId", product.getCategory().getId());
        } else {
            doc.put("categoryName", "");
            doc.put("categorySlug", "");
            doc.put("categoryId", 0L);
        }

        // Tags
        doc.put("tags", product.getTags() != null ? product.getTags() : List.of());

        // Rating
        doc.put("ratingAvg", product.getRatingAvg() != null ? product.getRatingAvg().doubleValue() : 0.0);
        doc.put("ratingCount", product.getRatingCount() != null ? product.getRatingCount() : 0);

        // Flags
        doc.put("isFeatured", Boolean.TRUE.equals(product.getIsFeatured()));
        doc.put("isActive", Boolean.TRUE.equals(product.getIsActive()));

        // Primary image
        String primaryImg = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImg = product.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImages().get(0).getImageUrl());
        }
        doc.put("primaryImageUrl", primaryImg != null ? primaryImg : "");

        // Seller
        doc.put("sellerId", product.getSeller() != null ? product.getSeller().getId() : 0L);
        doc.put("sellerName", product.getSeller() != null
                ? product.getSeller().getFirstName() + " " + product.getSeller().getLastName() : "");

        // Created at — unix timestamp (saniye cinsinden)
        doc.put("createdAt", product.getCreatedAt() != null
                ? product.getCreatedAt().toEpochSecond(ZoneOffset.UTC) : 0L);

        return doc;
    }

    private List<Long> extractIds(SearchResult result) {
        if (result.getHits() == null) return List.of();
        return result.getHits().stream()
                .map(hit -> {
                    Object idObj = hit.getDocument().get("id");
                    return Long.parseLong(String.valueOf(idObj));
                })
                .collect(Collectors.toList());
    }

    private String buildSortClause(String sortBy, String sortDir) {
        String direction = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt:desc";
        }
        return switch (sortBy) {
            case "price" -> "effectivePrice:" + direction;
            case "rating" -> "ratingAvg:" + direction;
            case "ratingCount" -> "ratingCount:" + direction;
            default -> "createdAt:" + direction;
        };
    }

    // ─── Sonuç DTO ──────────────────────────────────────────────────────────

    /**
     * Typesense arama sonuç container'ı.
     * ID listesi ve toplam sonuç sayısı tutar.
     */
    public record TypesenseSearchResult(List<Long> productIds, int totalFound) {}
}
