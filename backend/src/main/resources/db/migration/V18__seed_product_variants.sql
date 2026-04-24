-- V18__seed_product_variants.sql
SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- 6. product_variants (ID, product_id, color, color_hex, size, sku_variant, stock_quantity, price_modifier)
INSERT IGNORE INTO product_variants (product_id, color, color_hex, size, sku_variant, stock_quantity, price_modifier) VALUES
-- Tişörtler
(5, 'Siyah', '#000000', 'M', 'NK-DRFT-TST-BLK-M', 50, 0),
(5, 'Beyaz', '#FFFFFF', 'M', 'NK-DRFT-TST-WHT-M', 50, 0),
(5, 'Kırmızı', '#FF0000', 'M', 'NK-DRFT-TST-RED-M', 20, 0),

-- Spor Ayakkabılar (Nike Air Max 270 Koşu Ayakkabısı - 7)
(7, 'Siyah', '#000000', '42', 'NK-AM270-KAY-BLK-42', 30, 0),
(7, 'Kırmızı', '#FF0000', '42', 'NK-AM270-KAY-RED-42', 15, 0),
(7, 'Mavi', '#0000FF', '42', 'NK-AM270-KAY-BLU-42', 25, 0),

-- Sneaker (Converse Chuck - 9)
(9, 'Siyah', '#000000', '41', 'CNV-CT-HI-BLK-41', 20, 0),
(9, 'Beyaz', '#FFFFFF', '41', 'CNV-CT-HI-WHT-41', 20, 0),
(9, 'Kırmızı', '#FF0000', '41', 'CNV-CT-HI-RED-41', 10, 0),

-- Yeni Spor Ayakkabıları (ID 65-69)
(65, 'Siyah', '#000000', '43', 'NKE-SNK-AM270BW-BLK', 25, 0),
(65, 'Beyaz', '#FFFFFF', '43', 'NKE-SNK-AM270BW-WHT', 10, 0),
(65, 'Kırmızı', '#FF0000', '43', 'NKE-SNK-AM270BW-RED', 10, 0),

-- Akıllı Telefonlar (ID 38, 39)
(38, 'Obsidyen', '#000000', '256GB', 'GOG-PHN-PX8P-BLK', 20, 0),
(39, 'Siyah', '#000000', '256GB', 'OPL-PHN-OP12-BLK', 20, 0);

SET foreign_key_checks = 1;
