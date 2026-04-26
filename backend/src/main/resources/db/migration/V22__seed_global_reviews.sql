-- V22: Global Review Seeding and Rating Normalization
-- Populates average ratings and review counts for all products to enable healthy Analytics/Seller panel data

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- 1. Ensure we have diverse users to avoid UNIQUE constraint (user_id, product_id)
-- Using existing users: 1 (Admin), 2 (Demo User), 100-109 (Test Customers from V21)

-- 2. Seed Reviews for early products (1-34) that might have been empty
INSERT IGNORE INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at)
SELECT 
    p.id,
    u.id,
    CASE 
        WHEN (p.id + u.id) % 5 = 0 THEN 5
        WHEN (p.id + u.id) % 5 = 1 THEN 4
        WHEN (p.id + u.id) % 5 = 2 THEN 5
        WHEN (p.id + u.id) % 5 = 3 THEN 3
        ELSE 4
    END,
    'Kullanıcı Deneyimi',
    'Ürün beklentilerimi karşıladı, günlük kullanım için oldukça ideal.',
    TRUE,
    DATE_SUB(NOW(), INTERVAL (p.id % 60) DAY)
FROM products p
JOIN (SELECT id FROM users WHERE id IN (1, 2, 100, 101, 102, 103)) u
WHERE p.id BETWEEN 1 AND 34;

-- 3. Add extra "Critical" and "Top" reviews for distribution variety across all products (1-110)
INSERT IGNORE INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at)
SELECT 
    p.id,
    104 + (p.id % 5), -- Use users 104-108
    CASE 
        WHEN (p.id % 7) = 0 THEN 1
        WHEN (p.id % 7) = 1 THEN 2
        WHEN (p.id % 7) = 2 THEN 3
        ELSE 5
    END,
    'Genel Değerlendirme',
    'Deneyimlerime göre ürünün performansı bu şekilde.',
    TRUE,
    DATE_SUB(NOW(), INTERVAL (p.id % 20) DAY)
FROM products p
WHERE p.id BETWEEN 1 AND 110;

-- 4. CRITICAL: Recalculate and update rating_avg and rating_count in products table
-- This ensures the Seller Panel and Product List show correct values immediately
UPDATE products p
SET 
    rating_count = (SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id),
    rating_avg = (SELECT COALESCE(AVG(rating), 0) FROM reviews r WHERE r.product_id = p.id);

-- 5. Special Case: Ensure featured products have high ratings for a better UX
UPDATE products SET rating_avg = 4.85, rating_count = rating_count + 1 WHERE is_featured = TRUE AND rating_avg < 4.5;

SET foreign_key_checks = 1;
