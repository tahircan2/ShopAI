package com.shopai.config;

import com.shopai.entity.Product;
import com.shopai.repository.ProductRepository;
import com.shopai.service.TypesenseProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

            // 2. Tüm aktif ürünleri MySQL'den çek
            List<Product> activeProducts = productRepository.findAll()
                    .stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && !Boolean.TRUE.equals(p.getIsDeleted()))
                    .toList();

            if (activeProducts.isEmpty()) {
                log.info("  MySQL'de aktif ürün bulunamadı. Typesense boş başlıyor.");
            } else {
                // 3. Toplu indexle (batch'ler halinde — 100'erli)
                int batchSize = 100;
                for (int i = 0; i < activeProducts.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, activeProducts.size());
                    List<Product> batch = activeProducts.subList(i, end);
                    typesenseProductService.bulkIndex(batch);
                    log.info("  Batch indexlendi: {}/{}", end, activeProducts.size());
                }
                log.info("  ✓ Toplam {} ürün Typesense'e indexlendi.", activeProducts.size());
            }

            log.info("---------------------------------------------------------------");
            log.info("  Typesense senkronizasyonu tamamlandı ✓");
            log.info("---------------------------------------------------------------");
        } catch (Exception e) {
            log.error("══ Typesense senkronizasyon hatası ══", e);
            log.warn("  Uygulama Typesense olmadan çalışmaya devam edecek.");
            log.warn("  Arama MySQL LIKE fallback'ine düşecektir.");
        }
    }
}
