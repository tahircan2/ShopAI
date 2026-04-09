-- ShopAI E-Commerce — Consolidated Seed Data
-- This file combines V5, V7, V8, and V9 into a single consistent baseline.
SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- 1. Ensure SELLER role exists in ENUM
ALTER TABLE users MODIFY COLUMN role ENUM('USER', 'ADMIN', 'SELLER') NOT NULL DEFAULT 'USER';

-- Clean existing data to prevent duplicates (Users 1-3 only)
DELETE FROM product_images;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users WHERE id IN (1, 2, 3);

-- 2. Users (Admin password: Admin1234!, Demo password: User1234!, Seller password: Seller1234!)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, is_email_verified) VALUES
(1, 'admin@shopai.com', '$2a$12$N2/pg.pNXWIrKReE8qn/vuuNfNoXsMK9Y4VX3GosBk2AeJO66FqN2', 'ShopAI', 'Admin', 'ADMIN', TRUE, TRUE),
(2, 'demo@shopai.com', '$2a$12$TXYEYH8poA0EuqaaPzl5/eMsf9G44R1KV9jdsk8UiDGKIFy0leNq6', 'Demo', 'Kullanıcı', 'USER', TRUE, TRUE),
(3, 'seller@shopai.com', '$2a$12$ZB1PMnZgzsSSacbyK/bcG.ly0hbbdzSwM3tuNvChsaR5nLYYR2p9.', 'Demo', 'Satıcı', 'SELLER', TRUE, TRUE);

-- 3. Categories
INSERT INTO categories (id, name, slug, description, parent_id, is_active, sort_order) VALUES
(1, 'Elektronik', 'elektronik', 'Akıllı telefon, tablet, bilgisayar ve aksesuar', NULL, TRUE, 1),
(2, 'Giyim', 'giyim', 'Erkek, kadın ve çocuk giyim ürünleri', NULL, TRUE, 2),
(3, 'Ayakkabı', 'ayakkabi', 'Spor, günlük ve özel tasarım ayakkabılar', NULL, TRUE, 3),
(4, 'Ev & Yaşam', 'ev-yasam', 'Mobilya, dekorasyon ve ev tekstili', NULL, TRUE, 4),
(5, 'Spor & Outdoor', 'spor-outdoor', 'Spor ekipmanları, outdoor ürünler and fitness', NULL, TRUE, 5),
(6, 'Kitap & Hobi', 'kitap-hobi', 'Kitaplar, müzik aletleri ve hobi ürünleri', NULL, TRUE, 6),
(7, 'Akıllı Telefonlar', 'akilli-telefonlar', 'iOS ve Android akıllı telefonlar', 1, TRUE, 1),
(8, 'Laptop & PC', 'laptop-pc', 'Dizüstü ve masaüstü bilgisayarlar', 1, TRUE, 2),
(9, 'Tablet', 'tablet', 'iPad, Android tablet ve aksesuarlar', 1, TRUE, 3),
(10, 'Kulaklık', 'kulaklik', 'Kablosuz ve kablolu kulaklıklar', 1, TRUE, 4),
(11, 'Tişört', 'tisort', 'Erkek ve kadın tişörtler', 2, TRUE, 1),
(12, 'Sweatshirt', 'sweatshirt', 'Kapüşonlu ve basic sweatshirtler', 2, TRUE, 2),
(13, 'Pantolon', 'pantolon', 'Jean, chino ve spor pantolonlar', 2, TRUE, 3),
(15, 'Spor Ayakkabı', 'spor-ayakkabi', 'Koşu ve antrenman ayakkabıları', 3, TRUE, 1),
(16, 'Sneaker', 'sneaker', 'Günlük kullanım sneaker modelleri', 3, TRUE, 2);

-- 4. Products
INSERT INTO products (id, name, slug, description, long_description, price, discounted_price, stock_quantity, sku, category_id, brand, rating_avg, rating_count, is_active, is_featured, tags, meta_title, meta_description) VALUES
(1, 'iPhone 15 Pro 256GB', 'iphone-15-pro-256gb', 'A17 Pro çip, Titanyum tasarım', '<p>iPhone 15 Pro description...</p>', 54999.00, 49999.00, 45, 'APL-IP15P-256', 7, 'Apple', 4.80, 128, TRUE, TRUE, '[\"iphone\",\"apple\"]', 'iPhone 15 Pro', 'Desc'),
(2, 'Samsung Galaxy S24 Ultra 512GB', 'samsung-galaxy-s24-ultra-512gb', 'Snapdragon 8 Gen 3', '<p>S24 Ultra description...</p>', 47999.00, 44999.00, 30, 'SAM-S24U-512', 7, 'Samsung', 4.70, 89, TRUE, TRUE, '[\"samsung\"]', 'S24 Ultra', 'Desc'),
(3, 'MacBook Air M3 13\" 16GB/512GB', 'macbook-air-m3-13-16gb-512gb', 'M3 çip', '<p>MacBook Air description...</p>', 64999.00, NULL, 20, 'APL-MBA-M3-512', 8, 'Apple', 4.90, 64, TRUE, TRUE, '[\"macbook\"]', 'MacBook Air', 'Desc'),
(4, 'Sony WH-1000XM5 Kablosuz Kulaklık', 'sony-wh-1000xm5-kablosuz-kulaklik', 'ANC, 30 saat pil', '<p>Sony WH-1000XM5 desc...</p>', 8999.00, 7499.00, 80, 'SNY-WH1000XM5', 10, 'Sony', 4.75, 203, TRUE, FALSE, '[\"sony\"]', 'Sony WH', 'Desc'),
(5, 'Nike Dri-FIT Antrenman Tişörtü', 'nike-dri-fit-antrenman-tisortu', 'Nem emici', '<p>Nike Dri-FIT desc...</p>', 699.00, 549.00, 200, 'NK-DRFT-TST', 11, 'Nike', 4.50, 312, TRUE, FALSE, '[\"nike\"]', 'Nike Dri-FIT', 'Desc'),
(6, 'Adidas Essentials Logo Tişört', 'adidas-essentials-logo-tisortu', 'Klasik', '<p>Adidas Essentials desc...</p>', 599.00, NULL, 150, 'ADD-ESS-TST', 11, 'Adidas', 4.30, 178, TRUE, FALSE, '[\"adidas\"]', 'Adidas Essentials', 'Desc'),
(7, 'Nike Air Max 270 Koşu Ayakkabısı', 'nike-air-max-270-kosu-ayakkabisi', 'Air Max', '<p>Nike Air Max desc...</p>', 4499.00, 3999.00, 60, 'NK-AM270-KAY', 15, 'Nike', 4.65, 256, TRUE, TRUE, '[\"nike\"]', 'Nike Air Max', 'Desc'),
(8, 'Adidas Ultraboost 23 Koşu Ayakkabısı', 'adidas-ultraboost-23-kosu-ayakkabisi', 'BOOST', '<p>Adidas Ultraboost desc...</p>', 5299.00, 4799.00, 40, 'ADD-UB23-KAY', 15, 'Adidas', 4.80, 189, TRUE, TRUE, '[\"adidas\"]', 'Adidas Ultraboost', 'Desc'),
(9, 'Converse Chuck Taylor All Star Hi', 'converse-chuck-taylor-all-star-hi', 'İkonik', '<p>Converse desc...</p>', 1899.00, NULL, 120, 'CNV-CT-HI', 16, 'Converse', 4.40, 445, TRUE, FALSE, '[\"converse\"]', 'Converse Chuck', 'Desc'),
(11, 'Samsung Galaxy Watch 6 Classic', 'samsung-galaxy-watch-6-classic-akilli-saat', 'Smartwatch', '<p>Galaxy Watch 6 desc...</p>', 12499.00, 10999.00, 45, 'SAM-GW6CLS-47', 10, 'Samsung', 4.7, 218, TRUE, TRUE, '[\"samsung\"]', 'Galaxy Watch 6', 'Desc'),
(12, 'Apple AirPods Pro (2. Nesil)', 'apple-airpods-pro-2-nesil', 'AirPods', '<p>AirPods Pro desc...</p>', 9999.00, 8799.00, 120, 'APL-APP2-USBC', 10, 'Apple', 4.9, 589, TRUE, TRUE, '[\"apple\"]', 'AirPods Pro', 'Desc'),
(13, 'Lenovo IdeaPad Slim 5 Laptop', 'lenovo-ideapad-slim-5-laptop', 'Intel i7', '<p>Lenovo IdeaPad desc...</p>', 34999.00, 29999.00, 30, 'LNV-IPS5-I7-16', 8, 'Lenovo', 4.6, 156, TRUE, FALSE, '[\"lenovo\"]', 'IdeaPad Slim 5', 'Desc'),
(14, 'ASUS ROG Strix G16 Oyun Laptopu', 'asus-rog-strix-g16-oyun-laptopu', 'RTX 4070', '<p>ASUS ROG desc...</p>', 74999.00, 69999.00, 18, 'ASUS-ROG-G16-RTX4070', 8, 'ASUS', 4.8, 97, TRUE, TRUE, '[\"asus\"]', 'ROG Strix G16', 'Desc'),
(15, 'Xiaomi Redmi Pad SE Tablet', 'xiaomi-redmi-pad-se-tablet', 'Tablet', '<p>Xiaomi Redmi Pad desc...</p>', 7499.00, 6499.00, 90, 'XMI-RDMPADSE-128', 9, 'Xiaomi', 4.4, 203, TRUE, FALSE, '[\"xiaomi\"]', 'Redmi Pad SE', 'Desc'),
(18, 'New Balance 574 Sneaker', 'new-balance-574-sneaker', 'Retro', '<p>New Balance 574 desc...</p>', 3299.00, 2799.00, 180, 'NB-574-UNI', 15, 'New Balance', 4.6, 278, TRUE, FALSE, '[\"newbalance\"]', 'NB 574', 'Desc'),
(19, 'Puma RS-X Efekt Sneaker', 'puma-rs-x-efekt-sneaker', 'Chunky', '<p>Puma RS-X desc...</p>', 2999.00, 2499.00, 160, 'PUM-RSX-EFT-UNI', 15, 'Puma', 4.5, 192, TRUE, FALSE, '[\"puma\"]', 'Puma RS-X', 'Desc'),
(20, 'Mavi Jeans Slim Fit Erkek Pantolon', 'mavi-jeans-slim-fit-erkek-pantolon', 'Slim Fit', '<p>Mavi Jeans desc...</p>', 1299.00, 999.00, 300, 'MAV-SLIMFIT-MRK', 13, 'Mavi Jeans', 4.6, 512, TRUE, FALSE, '[\"mavijeans\"]', 'Mavi Slim Fit', 'Desc'),
(21, 'Levi''s 501 Original Straight Kot Pantolon', 'levis-501-original-straight-kot-pantolon', 'Straight Fit', '<p>Levi''s 501 desc...</p>', 1799.00, 1499.00, 220, 'LVS-501-ORG-STR', 13, 'Levi''s', 4.8, 678, TRUE, TRUE, '[\"levis\"]', 'Levi''s 501', 'Desc'),
(22, 'Tommy Hilfiger Essential Slim Tişört', 'tommy-hilfiger-essential-slim-tisort', 'Organic Cotton', '<p>Tommy Hilfiger desc...</p>', 799.00, 649.00, 500, 'TH-ESS-SLIM-TSH', 11, 'Tommy Hilfiger', 4.5, 389, TRUE, FALSE, '[\"tommy\"]', 'Tommy Essential', 'Desc'),
(23, 'Zara Oversize Baskılı Unisex Tişört', 'zara-oversize-baskili-unisex-tisort', 'Oversize', '<p>Zara Oversize desc...</p>', 649.00, 549.00, 400, 'ZAR-OVR-BSK-UNI', 11, 'Zara', 4.4, 261, TRUE, FALSE, '[\"zara\"]', 'Zara Oversize', 'Desc'),
(24, 'Champion Reverse Weave Hoodie Sweatshirt', 'champion-reverse-weave-hoodie-sweatshirt', 'Reverse Weave', '<p>Champion Hoodie desc...</p>', 1899.00, 1599.00, 180, 'CHP-RVW-HOOD-UNI', 12, 'Champion', 4.7, 298, TRUE, TRUE, '[\"champion\"]', 'Champion Hoodie', 'Desc'),
(25, 'Nike Tech Fleece Sweatshirt', 'nike-tech-fleece-sweatshirt', 'Tech Fleece', '<p>Nike Tech Fleece desc...</p>', 3499.00, 2999.00, 120, 'NK-TECHFLC-ZIP-UNI', 12, 'Nike', 4.8, 445, TRUE, TRUE, '[\"nike\"]', 'Nike Tech Fleece', 'Desc'),
(26, 'Ray-Ban Aviator Classic Güneş Gözlüğü', 'ray-ban-aviator-classic-gunes-gozlugu', 'Aviator', '<p>Ray-Ban Aviator desc...</p>', 4299.00, 3699.00, 85, 'RB-3025-AVTR-GOLD', 10, 'Ray-Ban', 4.9, 712, TRUE, TRUE, '[\"rayban\"]', 'Ray-Ban Aviator', 'Desc'),
(27, 'Samsonite Proxis Sırt Çantası', 'samsonite-proxis-sırt-cantasi', 'Backpack', '<p>Samsonite Proxis desc...</p>', 6999.00, 5999.00, 60, 'SAM-PRXS-BKP-BLK', 10, 'Samsonite', 4.7, 183, TRUE, FALSE, '[\"samsonite\"]', 'Samsonite Proxis', 'Desc'),
(28, 'Sony WH-1000XM5 Kablosuz Kulaklık (Gümüş Edition)', 'sony-wh-1000xm5-silver-edition', 'ANC Silver', '<p>Sony WH-1000XM5 Silver desc...</p>', 9499.00, 8499.00, 30, 'SNY-WH1000XM5-SLV', 10, 'Sony', 4.8, 45, TRUE, TRUE, '[\"sony\"]', 'Sony Silver', 'Desc'),
(29, 'Nike Air Max 270 Volt Koşu Ayakkabısı', 'nike-air-max-270-volt-edition', 'Volt Edition', '<p>Nike Volt desc...</p>', 4799.00, 4299.00, 40, 'NK-AM270-VOLT', 15, 'Nike', 4.7, 88, TRUE, FALSE, '[\"nike\"]', 'Nike Volt', 'Desc'),
(30, 'Adidas Ultraboost Light Koşu Ayakkabısı', 'adidas-ultraboost-light-v9', 'Ultraboost Light', '<p>Adidas Light desc...</p>', 5999.00, 5499.00, 55, 'ADD-UB-LIGHT-V9', 15, 'Adidas', 4.9, 112, TRUE, TRUE, '[\"adidas\"]', 'Adidas Light', 'Desc');

-- 5. Product Images
INSERT INTO product_images (product_id, image_url, alt_text, is_primary, sort_order) VALUES
(1, 'https://placehold.co/800x600/31343c/ffffff?text=iPhone+15+Pro', 'iPhone 15 Pro', TRUE, 0),
(2, 'https://placehold.co/800x600/31343c/ffffff?text=Galaxy+S24+Ultra', 'Galaxy S24 Ultra', TRUE, 0),
(3, 'https://placehold.co/800x600/31343c/ffffff?text=MacBook+Air+M3', 'MacBook Air M3', TRUE, 0),
(4, 'https://placehold.co/800x600/31343c/ffffff?text=Sony+WH-1000XM5', 'Sony WH-1000XM5', TRUE, 0),
(5, 'https://placehold.co/800x600/31343c/ffffff?text=Nike+Dri-FIT', 'Nike Dri-FIT', TRUE, 0),
(6, 'https://placehold.co/800x600/31343c/ffffff?text=Adidas+Essentials', 'Adidas Essentials', TRUE, 0),
(7, 'https://placehold.co/800x600/31343c/ffffff?text=Nike+Air+Max+270', 'Nike Air Max 270', TRUE, 1),
(8, 'https://placehold.co/800x600/31343c/ffffff?text=Adidas+Ultraboost', 'Adidas Ultraboost', TRUE, 2),
(9, 'https://placehold.co/800x600/31343c/ffffff?text=Converse+Chuck', 'Converse Chuck', TRUE, 0),
(11, 'https://placehold.co/800x600/31343c/ffffff?text=Galaxy+Watch+6', 'Galaxy Watch 6', TRUE, 0),
(12, 'https://placehold.co/800x600/31343c/ffffff?text=AirPods+Pro', 'AirPods Pro', TRUE, 0),
(13, 'https://placehold.co/800x600/31343c/ffffff?text=Lenovo+IdeaPad', 'Lenovo IdeaPad', TRUE, 0),
(14, 'https://placehold.co/800x600/31343c/ffffff?text=ASUS+ROG+Strix', 'ASUS ROG', TRUE, 0),
(15, 'https://placehold.co/800x600/31343c/ffffff?text=Redmi+Pad+SE', 'Redmi Pad SE', TRUE, 0),
(18, 'https://placehold.co/800x600/31343c/ffffff?text=New+Balance+574', 'New Balance 574', TRUE, 0),
(19, 'https://placehold.co/800x600/31343c/ffffff?text=Puma+RS-X', 'Puma RS-X', TRUE, 0),
(20, 'https://placehold.co/800x600/31343c/ffffff?text=Mavi+Jeans', 'Mavi Jeans', TRUE, 0),
(21, 'https://placehold.co/800x600/31343c/ffffff?text=Levis+501', 'Levi''s 501', TRUE, 0),
(22, 'https://placehold.co/800x600/31343c/ffffff?text=Tommy+Hilfiger', 'Tommy Hilfiger', TRUE, 0),
(23, 'https://placehold.co/800x600/31343c/ffffff?text=Zara+Oversize', 'Zara Oversize', TRUE, 0),
(24, 'https://placehold.co/800x600/31343c/ffffff?text=Champion+Hoodie', 'Champion Hoodie', TRUE, 0),
(25, 'https://placehold.co/800x600/31343c/ffffff?text=Nike+Tech+Fleece', 'Nike Tech Fleece', TRUE, 0),
(26, 'https://placehold.co/800x600/31343c/ffffff?text=Ray-Ban+Aviator', 'Ray-Ban Aviator', TRUE, 0),
(27, 'https://placehold.co/800x600/31343c/ffffff?text=Samsonite+Proxis', 'Samsonite Proxis', TRUE, 0),
(28, 'https://placehold.co/800x600/31343c/ffffff?text=Sony+WH-1000XM5+Silver', 'Sony Silver', TRUE, 0),
(29, 'https://placehold.co/800x600/31343c/ffffff?text=Nike+Air+Max+Volt', 'Nike Volt', TRUE, 0),
(30, 'https://placehold.co/800x600/31343c/ffffff?text=Adidas+Ultraboost+Light', 'Adidas Light', TRUE, 0);

SET foreign_key_checks = 1;