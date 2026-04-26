package com.shopai.config;

import com.shopai.entity.Product;
import com.shopai.repository.ProductRepository;
import com.shopai.service.TypesenseProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

/**
 * Uygulama başlangıcında Typesense koleksiyonunu oluşturur ve
 * MySQL'deki mevcut aktif ürünleri toplu olarak indexler.
 * <p>
 * Sadece typesense.enabled=true olduğunda çalışır.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "typesense.enabled", havingValue = "true", matchIfMissing = false)
public class TypesenseSyncRunner implements ApplicationRunner {

    private final TypesenseProductService typesenseProductService;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        log.info("---------------------------------------------------------------");
        log.info("  Typesense senkronizasyonu başlatılıyor...");
        log.info("---------------------------------------------------------------");

        try {
            // 1. Koleksiyonu oluştur (yoksa)
            typesenseProductService.ensureCollection();

            long totalProducts = productRepository.count();
            int batchSize = 100;
            int totalPages = (int) Math.ceil((double) totalProducts / batchSize);
            int indexedCount = 0;

            log.info("  MySQL'de toplam {} ürün bulundu. Indexleme başlıyor...", totalProducts);

            for (int i = 0; i < totalPages; i++) {
                Page<Product> productPage = productRepository.findAll(PageRequest.of(i, batchSize));
                List<Product> activeBatch = productPage.getContent().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                        .toList();

                if (!activeBatch.isEmpty()) {
                    typesenseProductService.bulkIndex(activeBatch);
                    indexedCount += activeBatch.size();
                }
                log.info("  Batch {}/{} tamamlandı. (Toplam {} ürün indexlendi)", i + 1, totalPages, indexedCount);
            }

            log.info("---------------------------------------------------------------");
            log.info("  Typesense senkronizasyonu tamamlandı. Toplam: {} ✓", indexedCount);
            log.info("---------------------------------------------------------------");
        } catch (Exception e) {
            log.error("══ Typesense senkronizasyon hatası ══", e);
            log.warn("  Uygulama Typesense olmadan çalışmaya devam edecek.");
            log.warn("  Arama MySQL LIKE fallback'ine düşecektir.");
        }
    }
}
