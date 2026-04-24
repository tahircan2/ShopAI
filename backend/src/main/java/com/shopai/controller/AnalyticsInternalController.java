package com.shopai.controller;

import com.shopai.exception.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Text2SQL Analytics Internal Endpoint.
 * Python AI servisinden gelen güvenli SELECT sorgularını çalıştırır.
 *
 * GÜVENLİK:
 * - X-Internal-Key ile korunur (sadece Python servisi erişebilir)
 * - SADECE SELECT sorgularına izin verir
 * - DML/DDL komutları (INSERT, UPDATE, DELETE, DROP) ENGELLENEN
 * - Sorgu sonuçları max 100 satırla sınırlı
 */
@RestController
@RequestMapping("/api/internal/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics Internal", description = "Text2SQL analytics sorgu çalıştırma (sadece Python servisi)")
public class AnalyticsInternalController {

    @PersistenceContext
    private EntityManager em;

    @Value("${app.ai-service.internal-key}")
    private String expectedInternalKey;

    // Tehlikeli SQL keyword'leri
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("\\bINSERT\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bUPDATE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDELETE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDROP\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bALTER\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bTRUNCATE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bCREATE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bGRANT\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bREVOKE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bEXEC(UTE)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bINTO\\s+OUTFILE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bLOAD\\s+DATA\\b", Pattern.CASE_INSENSITIVE)
    );

    @PostMapping("/query")
    @Operation(summary = "Text2SQL sorgusu çalıştır (sadece SELECT)")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> executeQuery(
            @RequestHeader("X-Internal-Key") String internalKey,
            @RequestBody Map<String, Object> body) {
        try {
            validateKey(internalKey);

            if (body == null) {
                log.error("Request body is null");
                return ResponseEntity.badRequest().body(Map.of("error", "Request body is null"));
            }

            log.info("Incoming analytics request. InternalKey present: {}, Body keys: {}", 
                internalKey != null, body.keySet());

            if (body.get("sql") == null || body.get("user_id") == null) {
                log.error("Missing fields in request body. Keys found: {}", body.keySet());
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "SQL or user_id is missing", 
                    "received_keys", body.keySet()
                ));
            }

            String sql = body.get("sql").toString();
            Long user_id = Long.valueOf(body.get("user_id").toString());

            log.info("Executing analytics query for user {}: {}", user_id, sql);

            sql = sql.strip();

            // Güvenlik kontrolü: SELECT ile başlamalı
            if (!sql.toUpperCase().startsWith("SELECT")) {
                log.warn("Non-SELECT query blocked: {}", sql.substring(0, Math.min(sql.length(), 100)));
                return ResponseEntity.status(403).body(Map.of("error", "Only SELECT queries allowed"));
            }

            // Tehlikeli keyword kontrolü
            for (Pattern pattern : BLOCKED_PATTERNS) {
                if (pattern.matcher(sql).find()) {
                    log.warn("Dangerous SQL blocked: pattern={}, sql={}", pattern.pattern(), sql.substring(0, Math.min(sql.length(), 100)));
                    return ResponseEntity.status(403).body(Map.of("error", "Dangerous SQL keyword detected"));
                }
            }

            // Native SQL query çalıştır
            var query = em.createNativeQuery(sql);

            List<Object[]> rawResults = query.getResultList();

            // Sonuçları Map listesine dönüştür
            // Column isimlerini almak için metadata olmadığından, col_0, col_1... kullanıyoruz
            // Ancak alias kullanıldığında NativeQuery metadata verir
            List<Map<String, Object>> results = new ArrayList<>();

            if (!rawResults.isEmpty()) {
                // Eğer tek sütun dönüyorsa
                if (rawResults.get(0) instanceof Object[]) {
                    // Column alias'larını SQL'den çıkarmaya çalış
                    List<String> aliases = extractAliases(sql);

                    for (Object[] row : rawResults) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        for (int i = 0; i < row.length; i++) {
                            String key = i < aliases.size() ? aliases.get(i) : "col_" + i;
                            map.put(key, row[i]);
                        }
                        results.add(map);
                    }
                } else {
                    // Tek sütunluk sonuç
                    List<String> aliases = extractAliases(sql);
                    String key = !aliases.isEmpty() ? aliases.get(0) : "value";
                    for (Object row : rawResults) {
                        results.add(Map.of(key, row != null ? row : ""));
                    }
                }
            }

            log.info("Analytics query executed: {} rows returned", results.size());
            return ResponseEntity.ok(results);

        } catch (Throwable e) {
            log.error("CRITICAL Analytics query error: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Query execution failed",
                    "detail", e.getMessage() != null ? e.getMessage() : "Internal server error during query execution"
            ));
        }
    }

    /**
     * SQL SELECT'ten alias isimlerini extract eder.
     * Örnek: SELECT COUNT(*) AS total, SUM(price) AS revenue FROM ...
     * → ["total", "revenue"]
     */
    private List<String> extractAliases(String sql) {
        List<String> aliases = new ArrayList<>();
        try {
            String upper = sql.toUpperCase().replaceAll("\\s+", " ");
            int fromIdx = upper.indexOf(" FROM ");
            if (fromIdx == -1) return aliases;

            String selectPart = sql.substring(0, fromIdx).trim();
            if (selectPart.toUpperCase().startsWith("SELECT ")) {
                selectPart = selectPart.substring(7);
            }

            // Basit virgül ayrımı
            String[] parts = selectPart.split(",");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                String partUpper = part.toUpperCase();
                int asIdx = partUpper.lastIndexOf(" AS ");
                
                if (asIdx != -1) {
                    String alias = part.substring(asIdx + 4).trim();
                    alias = alias.replace("`", "").replace("\"", "").replace("'", "");
                    aliases.add(alias);
                } else {
                    int lastSpace = part.lastIndexOf(" ");
                    if (lastSpace != -1) {
                        String potentialAlias = part.substring(lastSpace + 1).trim();
                        if (potentialAlias.matches("^[a-zA-Z0-9_]+$")) {
                            aliases.add(potentialAlias);
                        } else {
                            aliases.add("col_" + i);
                        }
                    } else {
                        aliases.add("col_" + i);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Alias extraction failed: {}", e.getMessage());
        }
        return aliases;
    }

    private void validateKey(String actualKey) {
        if (actualKey == null || !actualKey.equals(expectedInternalKey)) {
            throw new ForbiddenException("Geçersiz dahili anahtar");
        }
    }
}
