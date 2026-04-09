-- V10: Add seller_id to products and link to users
-- Existing products are assigned to the admin user (id=1) for backward compatibility

ALTER TABLE products ADD COLUMN seller_id BIGINT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_seller
        FOREIGN KEY (seller_id) REFERENCES users(id)
            ON DELETE SET NULL;

CREATE INDEX idx_products_seller ON products(seller_id);

-- Assign existing products to the first admin user
UPDATE products p
SET p.seller_id = (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id ASC LIMIT 1)
WHERE p.seller_id IS NULL;
