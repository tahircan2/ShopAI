-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V3: AI, Audit, Session, Wishlist, Notification Tables
-- ============================================================

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- ============================================================
-- 14. ai_conversations
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_conversations (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NULL      COMMENT 'NULL = anonim kullanıcı',
    session_id      VARCHAR(100)    NOT NULL  COMMENT 'Frontend UUID',
    started_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME        NULL,
    message_count   INT             NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_conversations_session (session_id),
    CONSTRAINT fk_ai_conversations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI chatbot konuşma oturumları';

-- ============================================================
-- 15. ai_messages
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_messages (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_id         BIGINT      NOT NULL,
    role                    ENUM('user','assistant','system') NOT NULL,
    content                 TEXT        NOT NULL,
    agent_type              VARCHAR(50) NULL  COMMENT 'filter_agent, cart_agent vb.',
    action_type             VARCHAR(50) NULL  COMMENT 'PRODUCT_LIST, CART_UPDATED, NAVIGATE, INFO',
    action_data             JSON        NULL  COMMENT 'Agent aksiyonu için yapılandırılmış veri',
    tokens_used             INT         NULL,
    processing_ms           INT         NULL  COMMENT 'Yanıt süresi (ms)',
    is_injection_detected   BOOLEAN     NOT NULL DEFAULT FALSE
                                              COMMENT 'Prompt injection tespiti — 3 katmanlı güvenlik',
    created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_ai_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES ai_conversations (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI chatbot mesajları (token ve injection takibi dahil)';

-- ============================================================
-- 16. audit_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NULL      COMMENT 'NULL = anonim işlem',
    action      VARCHAR(100)    NOT NULL  COMMENT 'USER_LOGIN, ORDER_CREATED, INJECTION_DETECTED vb.',
    entity_type VARCHAR(50)     NULL      COMMENT 'User, Order, Product vb.',
    entity_id   BIGINT          NULL,
    old_data    JSON            NULL      COMMENT 'Değişim öncesi veri',
    new_data    JSON            NULL      COMMENT 'Değişim sonrası veri',
    ip_address  VARCHAR(45)     NULL,
    user_agent  VARCHAR(500)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    -- user_id için kısıtlı FK: kullanıcı silindiğinde log korunur (SET NULL değil RESTRICT değil — NULL bırak)
    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Tüm kritik işlemlerin değişmez audit kaydı';

-- ============================================================
-- 17. user_sessions
-- ============================================================
CREATE TABLE IF NOT EXISTS user_sessions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    session_token_hash  VARCHAR(255)    NOT NULL,
    ip_address          VARCHAR(45)     NULL,
    device_info         TEXT            NULL      COMMENT 'Browser, OS, cihaz bilgisi',
    login_at            DATETIME        NOT NULL,
    logout_at           DATETIME        NULL,
    expires_at          DATETIME        NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Oturum kayıtları (JWT HttpOnly cookie ile yönetilir)';

-- ============================================================
-- 18. wishlist_items
-- ============================================================
CREATE TABLE IF NOT EXISTS wishlist_items (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    added_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    -- Aynı ürünü iki kez favoriye eklenemez
    UNIQUE KEY uq_wishlist_user_product (user_id, product_id),
    CONSTRAINT fk_wishlist_items_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_wishlist_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Kullanıcı favori listesi';

-- ============================================================
-- 19. notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    type            ENUM('ORDER_STATUS','PROMOTION','SYSTEM') NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    message         TEXT            NOT NULL,
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    reference_id    BIGINT          NULL      COMMENT 'İlgili kayıt ID (sipariş, ürün vb.)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Kullanıcı bildirimleri';

SET foreign_key_checks = 1;
