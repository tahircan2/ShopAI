-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V2: Commerce Tables (reviews, carts, orders, addresses, coupons)
-- ============================================================

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- ============================================================
-- 7. reviews
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    product_id          BIGINT      NOT NULL,
    user_id             BIGINT      NOT NULL,
    rating              INT         NOT NULL,
    title               VARCHAR(255) NULL,
    comment             TEXT        NULL,
    is_verified_purchase BOOLEAN    NOT NULL DEFAULT FALSE,
    is_approved         BOOLEAN     NOT NULL DEFAULT TRUE,
    helpful_count       INT         NOT NULL DEFAULT 0,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_reviews_rating
        CHECK (rating BETWEEN 1 AND 5),
    -- Bir kullanıcı aynı ürüne yalnızca bir yorum yapabilir
    UNIQUE KEY uq_reviews_user_product (user_id, product_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Ürün yorumları ve puanları';

-- ============================================================
-- 8. carts
-- ============================================================
CREATE TABLE IF NOT EXISTS carts (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    -- Kullanıcı başına tek sepet
    UNIQUE KEY uq_carts_user (user_id),
    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Kullanıcı sepetleri';

-- ============================================================
-- 9. cart_items
-- ============================================================
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    cart_id     BIGINT          NOT NULL,
    product_id  BIGINT          NOT NULL,
    variant_id  BIGINT          NULL      COMMENT 'Seçilen varyant; NULL = varyantsız',
    quantity    INT             NOT NULL,
    price_at_add DECIMAL(10,2)  NOT NULL  COMMENT 'Sepete eklendiğindeki fiyat — değişmez',
    added_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_items_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Sepet kalemleri';

-- ============================================================
-- 10. address
-- ============================================================
CREATE TABLE IF NOT EXISTS address (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    label           VARCHAR(50)     NULL      COMMENT 'Ev, İş vb.',
    full_name       VARCHAR(150)    NOT NULL,
    phone           VARCHAR(20)     NULL,
    address_line1   VARCHAR(255)    NOT NULL,
    address_line2   VARCHAR(255)    NULL,
    city            VARCHAR(100)    NOT NULL,
    district        VARCHAR(100)    NULL,
    postal_code     VARCHAR(10)     NULL,
    country         VARCHAR(50)     NOT NULL DEFAULT 'Türkiye',
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),
    CONSTRAINT fk_address_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Teslimat ve faturalama adresleri';

-- ============================================================
-- 11. coupons
-- ============================================================
CREATE TABLE IF NOT EXISTS coupons (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)     NOT NULL,
    discount_type       ENUM('PERCENTAGE','FIXED') NOT NULL,
    discount_value      DECIMAL(10,2)   NOT NULL,
    min_order_amount    DECIMAL(10,2)   NULL,
    max_uses            INT             NULL,
    used_count          INT             NOT NULL DEFAULT 0,
    valid_from          DATETIME        NOT NULL,
    valid_until         DATETIME        NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE KEY uq_coupons_code (code),
    CONSTRAINT chk_coupons_discount_value
        CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_dates
        CHECK (valid_until > valid_from),
    CONSTRAINT chk_coupons_percentage
        CHECK (discount_type != 'PERCENTAGE' OR discount_value <= 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='İndirim kuponları';

-- ============================================================
-- 12. orders
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_number        VARCHAR(20)     NOT NULL  COMMENT 'ORD-20240101-XXXX formatı',
    user_id             BIGINT          NOT NULL,
    status              ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED','REFUNDED')
                                        NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(10,2)   NOT NULL,
    tax_amount          DECIMAL(10,2)   NOT NULL  COMMENT '%18 KDV',
    shipping_cost       DECIMAL(10,2)   NOT NULL,
    discount_amount     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(10,2)   NOT NULL,
    coupon_code         VARCHAR(50)     NULL,
    shipping_address_id BIGINT          NULL,
    notes               TEXT            NULL,
    payment_status      ENUM('PENDING','PAID','FAILED','REFUNDED')
                                        NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(50)     NULL,
    payment_reference   VARCHAR(255)    NULL,
    shipped_at          DATETIME        NULL,
    delivered_at        DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_orders_order_number (order_number),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_address
        FOREIGN KEY (shipping_address_id) REFERENCES address (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_orders_amounts
        CHECK (subtotal >= 0 AND tax_amount >= 0 AND shipping_cost >= 0
               AND discount_amount >= 0 AND total_amount >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Müşteri siparişleri';

-- ============================================================
-- 13. order_items
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    product_id      BIGINT          NULL      COMMENT 'Soft reference; ürün silinse de kayıt kalır',
    variant_id      BIGINT          NULL      COMMENT 'Sipariş anındaki varyant (snapshot)',
    product_name    VARCHAR(255)    NOT NULL  COMMENT 'Anlık ürün adı snapshot',
    product_sku     VARCHAR(100)    NULL      COMMENT 'Anlık SKU snapshot',
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(10,2)   NOT NULL  COMMENT 'Sipariş anındaki birim fiyat',
    total_price     DECIMAL(10,2)   NOT NULL  COMMENT 'quantity * unit_price',

    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price
        CHECK (unit_price >= 0 AND total_price >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Sipariş kalemleri (fiyat snapshot ile)';

SET foreign_key_checks = 1;
