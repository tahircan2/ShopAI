-- V21: High-Fidelity Analytics Seeding for ShopAI
-- Focus: Realistic Sales Trends, Product Distribution, and Customer Feedback for Seller (id=3)

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- 1. Ensure Schema Consistency
-- MySQL 8.0 does not support 'ADD COLUMN IF NOT EXISTS'. Using a safe procedure.
DROP PROCEDURE IF EXISTS AddShippingMode;
DELIMITER //
CREATE PROCEDURE AddShippingMode()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'shipping_mode'
    ) THEN
        ALTER TABLE orders ADD COLUMN shipping_mode ENUM('Air', 'Road', 'Ship') NOT NULL DEFAULT 'Road';
    END IF;
END //
DELIMITER ;
CALL AddShippingMode();
DROP PROCEDURE AddShippingMode;

-- 2. Target Account: seller@shopai.com (id=3)
-- Link all extended products from V16, V18, V20 to this seller
UPDATE products SET seller_id = 3 WHERE id BETWEEN 35 AND 110;

-- 3. Create Diverse Test Users for Analytics (id 100-110)
INSERT IGNORE INTO users (id, email, password_hash, first_name, last_name, role, is_active, is_email_verified) VALUES
(100, 'customer.1@example.com', 'hash', 'Ahmet', 'Yılmaz', 'USER', TRUE, TRUE),
(101, 'customer.2@example.com', 'hash', 'Ayşe', 'Demir', 'USER', TRUE, TRUE),
(102, 'customer.3@example.com', 'hash', 'Mehmet', 'Kaya', 'USER', TRUE, TRUE),
(103, 'customer.4@example.com', 'hash', 'Fatma', 'Şahin', 'USER', TRUE, TRUE),
(104, 'customer.5@example.com', 'hash', 'Can', 'Öztürk', 'USER', TRUE, TRUE),
(105, 'customer.6@example.com', 'hash', 'Zeynep', 'Aydın', 'USER', TRUE, TRUE),
(106, 'customer.7@example.com', 'hash', 'Burak', 'Arslan', 'USER', TRUE, TRUE),
(107, 'customer.8@example.com', 'hash', 'Elif', 'Yıldız', 'USER', TRUE, TRUE),
(108, 'customer.9@example.com', 'hash', 'Murat', 'Çelik', 'USER', TRUE, TRUE),
(109, 'customer.10@example.com', 'hash', 'Selin', 'Koç', 'USER', TRUE, TRUE);

-- 4. Clear Old Data for these products to avoid constraint/logic conflicts
DELETE FROM order_items WHERE product_id BETWEEN 35 AND 110;
DELETE FROM reviews WHERE product_id BETWEEN 35 AND 110;

-- 5. SEED: Star Rating Distribution (150+ Reviews)
-- Using a mix of users and products to create a realistic bell curve
INSERT INTO reviews (product_id, user_id, rating, title, comment, is_verified_purchase, created_at)
SELECT 
    p.id,
    u.id,
    CASE 
        WHEN (p.id + u.id) % 10 IN (0, 1) THEN 5
        WHEN (p.id + u.id) % 10 IN (2, 3) THEN 4
        WHEN (p.id + u.id) % 10 IN (4)    THEN 3
        WHEN (p.id + u.id) % 10 IN (5)    THEN 2
        WHEN (p.id + u.id) % 10 IN (6)    THEN 1
        ELSE 5
    END,
    'Değerlendirme',
    'Ürün kalitesi ve teslimat hızı hakkında geri bildirim.',
    TRUE,
    DATE_SUB(NOW(), INTERVAL (p.id % 45) DAY)
FROM products p
JOIN (SELECT id FROM users WHERE id BETWEEN 100 AND 109) u
WHERE p.id BETWEEN 35 AND 84 AND (p.id + u.id) % 3 = 0;

-- 6. SEED: Orders & Revenue Trends (Last 30 Days)
-- We need daily data points for "geçen haftaki günlük gelir trendi"
-- And status distribution for "sipariş durumu dağılımı"
-- And shipping modes for "sevkiyat modu dağılımı"

-- Clear existing IDs if we're re-running (safe range 5000-6000)
DELETE FROM orders WHERE id BETWEEN 5000 AND 5100;

-- Generate ~60 orders across various dates
INSERT INTO orders (id, order_number, user_id, status, subtotal, tax_amount, shipping_cost, discount_amount, total_amount, shipping_mode, created_at, updated_at)
SELECT 
    5000 + n,
    CONCAT('ORD-2024-', 10000 + n),
    100 + (n % 10),
    CASE 
        WHEN n % 10 < 7 THEN 'DELIVERED'
        WHEN n % 10 = 7 THEN 'SHIPPED'
        WHEN n % 10 = 8 THEN 'PENDING'
        ELSE 'CANCELLED'
    END,
    (200 + (n * 150.50)),
    (n * 27.09),
    50.00,
    CASE WHEN n % 5 = 0 THEN 100.00 ELSE 0.00 END,
    (250 + (n * 177.59)),
    CASE 
        WHEN n % 3 = 0 THEN 'Air'
        WHEN n % 3 = 1 THEN 'Road'
        ELSE 'Ship'
    END,
    DATE_SUB(CURDATE(), INTERVAL (n % 30) DAY),
    DATE_SUB(CURDATE(), INTERVAL (n % 30) DAY)
FROM (
    SELECT a.N + b.N * 10 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) b
) numbers;

-- 7. SEED: Order Items (Links Orders to Seller 3's Products)
INSERT INTO order_items (order_id, product_id, product_name, product_sku, quantity, unit_price, total_price)
SELECT 
    o.id,
    p.id,
    p.name,
    p.sku,
    1 + (o.id % 3),
    p.price,
    p.price * (1 + (o.id % 3))
FROM orders o
JOIN products p ON p.id = (35 + (o.id % 50))
WHERE o.id BETWEEN 5000 AND 5100;

-- 8. Fix existing null total_amounts if any
UPDATE orders SET total_amount = subtotal + tax_amount + shipping_cost - discount_amount WHERE total_amount = 0;

SET foreign_key_checks = 1;
