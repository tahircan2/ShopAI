-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V1: Core Tables (users, refresh_tokens, categories, products)
-- Security-Hardened Edition | UTF8MB4 | InnoDB
-- ============================================================

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- ============================================================
-- 1. users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL                  COMMENT 'BCrypt hashed password',
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    phone                   VARCHAR(20)     NULL,
    role                    ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER'
                                                          COMMENT 'Yetki seviyesi — backend JWT imzasıyla korunur, client değeri kabul edilmez',
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified       BOOLEAN         NOT NULL DEFAULT FALSE,
    email_verify_token      VARCHAR(255)    NULL,
    password_reset_token    VARCHAR(255)    NULL      COMMENT 'BCrypt hash olarak saklanır',
    password_reset_expires  DATETIME        NULL,
    failed_login_attempts   INT             NOT NULL DEFAULT 0,
    locked_until            DATETIME        NULL      COMMENT '5 başarısız giriş → 15dk kilit',
    last_login_at           DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Kullanıcı hesapları';

-- ============================================================
-- 2. refresh_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL  COMMENT 'SHA-256 hash — plain token asla saklanmaz',
    device_info VARCHAR(255)    NULL,
    ip_address  VARCHAR(45)     NULL      COMMENT 'IPv4 veya IPv6',
    expires_at  DATETIME        NOT NULL,
    revoked_at  DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_hash (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='HttpOnly cookie ile taşınan refresh token hash kayıtları';

-- ============================================================
-- 3. categories
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    slug        VARCHAR(100)    NOT NULL  COMMENT 'URL dostu isim',
    description TEXT            NULL,
    parent_id   BIGINT          NULL      COMMENT 'Alt kategori için üst referans (self-join)',
    image_url   VARCHAR(500)    NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_slug (slug),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Hiyerarşik ürün kategorileri (self-referencing)';

-- ============================================================
-- 4. products
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)    NOT NULL,
    slug                VARCHAR(255)    NOT NULL  COMMENT 'SEO-friendly URL slug',
    description         TEXT            NULL,
    long_description    LONGTEXT        NULL      COMMENT 'Detaylı açıklama (HTML içerik)',
    price               DECIMAL(10,2)   NOT NULL,
    discounted_price    DECIMAL(10,2)   NULL,
    stock_quantity      INT             NOT NULL DEFAULT 0,
    sku                 VARCHAR(100)    NULL,
    category_id         BIGINT          NULL,
    brand               VARCHAR(100)    NULL,
    rating_avg          DECIMAL(3,2)    NOT NULL DEFAULT 0.00,
    rating_count        INT             NOT NULL DEFAULT 0,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_featured         BOOLEAN         NOT NULL DEFAULT FALSE,
    tags                JSON            NULL      COMMENT 'AI arama için etiket dizisi',
    meta_title          VARCHAR(255)    NULL,
    meta_description    VARCHAR(500)    NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_products_slug (slug),
    UNIQUE KEY uq_products_sku (sku),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_products_price
        CHECK (price >= 0),
    CONSTRAINT chk_products_discounted_price
        CHECK (discounted_price IS NULL OR discounted_price >= 0),
    CONSTRAINT chk_products_rating_avg
        CHECK (rating_avg >= 0.00 AND rating_avg <= 5.00),
    CONSTRAINT chk_products_stock
        CHECK (stock_quantity >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Ürün kataloğu';

-- ============================================================
-- 5. product_images
-- ============================================================
CREATE TABLE IF NOT EXISTS product_images (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    image_url   VARCHAR(500)    NOT NULL,
    alt_text    VARCHAR(255)    NULL,
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order  INT             NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Ürün görsel galerisi';

-- ============================================================
-- 6. product_variants
-- ============================================================
CREATE TABLE IF NOT EXISTS product_variants (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL,
    color           VARCHAR(50)     NULL,
    color_hex       VARCHAR(7)      NULL      COMMENT 'Örn: #FF0000',
    size            VARCHAR(20)     NULL      COMMENT 'S, M, L, XL vb.',
    sku_variant     VARCHAR(100)    NULL,
    stock_quantity  INT             NOT NULL DEFAULT 0,
    price_modifier  DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT 'Ana fiyata eklenen fark',

    PRIMARY KEY (id),
    UNIQUE KEY uq_product_variants_sku (sku_variant),
    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_variants_stock
        CHECK (stock_quantity >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Ürün renk/beden varyantları';

SET foreign_key_checks = 1;
