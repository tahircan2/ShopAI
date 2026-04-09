-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V6: Triggers, Stored Procedures & Events
-- ============================================================

SET NAMES utf8mb4;
DELIMITER $$

-- ============================================================
-- TRIGGER: Yorum eklendikten sonra ürün rating güncelle
-- ============================================================
DROP TRIGGER IF EXISTS trg_reviews_after_insert$$
CREATE TRIGGER trg_reviews_after_insert
    AFTER INSERT ON reviews
    FOR EACH ROW
BEGIN
    UPDATE products
    SET rating_avg   = (
            SELECT ROUND(AVG(rating), 2)
            FROM reviews
            WHERE product_id = NEW.product_id AND is_approved = TRUE
        ),
        rating_count = (
            SELECT COUNT(*)
            FROM reviews
            WHERE product_id = NEW.product_id AND is_approved = TRUE
        )
    WHERE id = NEW.product_id;
END$$

-- ============================================================
-- TRIGGER: Yorum silindikten sonra ürün rating güncelle
-- ============================================================
DROP TRIGGER IF EXISTS trg_reviews_after_delete$$
CREATE TRIGGER trg_reviews_after_delete
    AFTER DELETE ON reviews
    FOR EACH ROW
BEGIN
    UPDATE products
    SET rating_avg   = COALESCE((
            SELECT ROUND(AVG(rating), 2)
            FROM reviews
            WHERE product_id = OLD.product_id AND is_approved = TRUE
        ), 0.00),
        rating_count = (
            SELECT COUNT(*)
            FROM reviews
            WHERE product_id = OLD.product_id AND is_approved = TRUE
        )
    WHERE id = OLD.product_id;
END$$

-- ============================================================
-- TRIGGER: Yorum güncellendiğinde ürün rating güncelle
-- ============================================================
DROP TRIGGER IF EXISTS trg_reviews_after_update$$
CREATE TRIGGER trg_reviews_after_update
    AFTER UPDATE ON reviews
    FOR EACH ROW
BEGIN
    IF OLD.rating != NEW.rating OR OLD.is_approved != NEW.is_approved THEN
        UPDATE products
        SET rating_avg   = COALESCE((
                SELECT ROUND(AVG(rating), 2)
                FROM reviews
                WHERE product_id = NEW.product_id AND is_approved = TRUE
            ), 0.00),
            rating_count = (
                SELECT COUNT(*)
                FROM reviews
                WHERE product_id = NEW.product_id AND is_approved = TRUE
            )
        WHERE id = NEW.product_id;
    END IF;
END$$

-- ============================================================
-- TRIGGER: Sipariş oluştuğunda ürün stoğunu düş
-- (Spring Boot OrderService ile paralel çalışır — double-check)
-- ============================================================
DROP TRIGGER IF EXISTS trg_order_items_after_insert$$
CREATE TRIGGER trg_order_items_after_insert
    AFTER INSERT ON order_items
    FOR EACH ROW
BEGIN
    -- Ana ürün stoğunu düş (her zaman)
    UPDATE products
    SET stock_quantity = stock_quantity - NEW.quantity
    WHERE id = NEW.product_id;

    -- Varyant stoğunu düş (varsa)
    IF NEW.variant_id IS NOT NULL THEN
        UPDATE product_variants
        SET stock_quantity = stock_quantity - NEW.quantity
        WHERE id = NEW.variant_id;
    END IF;
END$$

-- ============================================================
-- TRIGGER: Sipariş iptal edildiğinde stoğu geri yükle
-- (status CANCELLED veya REFUNDED olduğunda)
-- ============================================================
DROP TRIGGER IF EXISTS trg_orders_after_update$$
CREATE TRIGGER trg_orders_after_update
    AFTER UPDATE ON orders
    FOR EACH ROW
BEGIN
    -- Yalnızca CANCELLED veya REFUNDED durumuna geçişte çalış
    IF NEW.status IN ('CANCELLED', 'REFUNDED')
       AND OLD.status NOT IN ('CANCELLED', 'REFUNDED') THEN

        -- Sipariş kalemlerindeki tüm ürünlerin stoğunu geri yükle
        UPDATE products p
        INNER JOIN order_items oi ON oi.product_id = p.id
        SET p.stock_quantity = p.stock_quantity + oi.quantity
        WHERE oi.order_id = NEW.id;

        -- Varyant stoklarını geri yükle
        UPDATE product_variants pv
        INNER JOIN order_items oi ON oi.variant_id = pv.id
        SET pv.stock_quantity = pv.stock_quantity + oi.quantity
        WHERE oi.order_id = NEW.id AND oi.variant_id IS NOT NULL;

    END IF;
END$$

-- ============================================================
-- TRIGGER: Kupon kullanıldığında used_count artır
-- ============================================================
DROP TRIGGER IF EXISTS trg_orders_coupon_after_insert$$
CREATE TRIGGER trg_orders_coupon_after_insert
    AFTER INSERT ON orders
    FOR EACH ROW
BEGIN
    IF NEW.coupon_code IS NOT NULL THEN
        UPDATE coupons
        SET used_count = used_count + 1
        WHERE code = NEW.coupon_code;
    END IF;
END$$

-- ============================================================
-- TRIGGER: ai_conversations mesaj sayısını ve son mesaj zamanını güncelle
-- ============================================================
DROP TRIGGER IF EXISTS trg_ai_messages_after_insert$$
CREATE TRIGGER trg_ai_messages_after_insert
    AFTER INSERT ON ai_messages
    FOR EACH ROW
BEGIN
    UPDATE ai_conversations
    SET message_count   = message_count + 1,
        last_message_at = NEW.created_at
    WHERE id = NEW.conversation_id;
END$$

-- ============================================================
-- STORED PROCEDURE: Siparişi sepetten oluştur
-- Kullanım: CALL sp_create_order_from_cart(userId, addressId, notes, paymentMethod, @orderNumber)
-- Spring Boot OrderService bu prosedürü çağırabilir VEYA kendi mantığını uygulayabilir
-- ============================================================
DROP PROCEDURE IF EXISTS sp_create_order_from_cart$$
CREATE PROCEDURE sp_create_order_from_cart(
    IN  p_user_id           BIGINT,
    IN  p_address_id        BIGINT,
    IN  p_notes             TEXT,
    IN  p_payment_method    VARCHAR(50),
    OUT p_order_number      VARCHAR(20)
)
BEGIN
    DECLARE v_cart_id       BIGINT;
    DECLARE v_subtotal      DECIMAL(10,2) DEFAULT 0;
    DECLARE v_tax_amount    DECIMAL(10,2);
    DECLARE v_shipping_cost DECIMAL(10,2);
    DECLARE v_total         DECIMAL(10,2);
    DECLARE v_order_id      BIGINT;
    DECLARE v_order_num     VARCHAR(20);

    -- Hata durumunda rollback
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Kullanıcının sepetini bul
    SELECT id INTO v_cart_id FROM carts WHERE user_id = p_user_id LIMIT 1;

    IF v_cart_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sepet bulunamadı';
    END IF;

    -- Subtotal hesapla
    SELECT SUM(ci.quantity * ci.price_at_add)
    INTO v_subtotal
    FROM cart_items ci
    WHERE ci.cart_id = v_cart_id;

    IF v_subtotal IS NULL OR v_subtotal = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sepet boş';
    END IF;

    -- KDV %18
    SET v_tax_amount = ROUND(v_subtotal * 0.18, 2);
    -- Kargo: 500₺ altı 49.90₺, üstü ücretsiz
    SET v_shipping_cost = IF(v_subtotal >= 500, 0.00, 49.90);
    SET v_total = v_subtotal + v_tax_amount + v_shipping_cost;

    -- Sipariş numarası üret: ORD-YYYYMMDD-RANDOM
    SET v_order_num = CONCAT('ORD-', DATE_FORMAT(NOW(), '%Y%m%d'), '-',
                             UPPER(SUBSTRING(MD5(RAND()), 1, 6)));
    SET p_order_number = v_order_num;

    -- Sipariş kaydı oluştur
    INSERT INTO orders
        (order_number, user_id, status, subtotal, tax_amount, shipping_cost,
         discount_amount, total_amount, shipping_address_id, notes, payment_method)
    VALUES
        (v_order_num, p_user_id, 'PENDING', v_subtotal, v_tax_amount, v_shipping_cost,
         0.00, v_total, p_address_id, p_notes, p_payment_method);

    SET v_order_id = LAST_INSERT_ID();

    -- Sepet kalemlerini sipariş kalemlerine kopyala
    INSERT INTO order_items
        (order_id, product_id, variant_id, product_name, product_sku, quantity, unit_price, total_price)
    SELECT
        v_order_id,
        ci.product_id,
        ci.variant_id,
        p.name,
        COALESCE(pv.sku_variant, p.sku),
        ci.quantity,
        ci.price_at_add,
        ci.quantity * ci.price_at_add
    FROM cart_items ci
    INNER JOIN products p  ON p.id = ci.product_id
    LEFT  JOIN product_variants pv ON pv.id = ci.variant_id
    WHERE ci.cart_id = v_cart_id;

    -- Sepeti temizle
    DELETE FROM cart_items WHERE cart_id = v_cart_id;

    COMMIT;
END$$

-- ============================================================
-- STORED PROCEDURE: Süresi dolmuş token'ları temizle
-- Kullanım: CALL sp_cleanup_expired_tokens()
-- Spring Boot TokenCleanupScheduler bu prosedürü çağırır
-- ============================================================
DROP PROCEDURE IF EXISTS sp_cleanup_expired_tokens$$
CREATE PROCEDURE sp_cleanup_expired_tokens()
BEGIN
    DECLARE v_deleted_refresh INT DEFAULT 0;
    DECLARE v_deleted_sessions INT DEFAULT 0;

    -- Süresi dolmuş refresh token'ları sil
    DELETE FROM refresh_tokens
    WHERE expires_at < NOW()
       OR revoked_at IS NOT NULL;

    SET v_deleted_refresh = ROW_COUNT();

    -- Süresi dolmuş user session'ları pasife al
    UPDATE user_sessions
    SET is_active = FALSE
    WHERE expires_at < NOW() AND is_active = TRUE;

    SET v_deleted_sessions = ROW_COUNT();

    SELECT v_deleted_refresh AS deleted_refresh_tokens,
           v_deleted_sessions AS deactivated_sessions;
END$$

-- ============================================================
-- STORED PROCEDURE: Kupon geçerlilik kontrolü
-- Kullanım: CALL sp_validate_coupon(couponCode, orderSubtotal, userId, @discount, @errorMsg)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_validate_coupon$$
CREATE PROCEDURE sp_validate_coupon(
    IN  p_code          VARCHAR(50),
    IN  p_subtotal      DECIMAL(10,2),
    IN  p_user_id       BIGINT,
    OUT p_discount      DECIMAL(10,2),
    OUT p_error_msg     VARCHAR(255)
)
BEGIN
    DECLARE v_discount_type     ENUM('PERCENTAGE','FIXED');
    DECLARE v_discount_value    DECIMAL(10,2);
    DECLARE v_min_order         DECIMAL(10,2);
    DECLARE v_max_uses          INT;
    DECLARE v_used_count        INT;
    DECLARE v_valid_from        DATETIME;
    DECLARE v_valid_until       DATETIME;
    DECLARE v_is_active         BOOLEAN;

    SET p_discount  = 0.00;
    SET p_error_msg = NULL;

    SELECT discount_type, discount_value, min_order_amount,
           max_uses, used_count, valid_from, valid_until, is_active
    INTO v_discount_type, v_discount_value, v_min_order,
         v_max_uses, v_used_count, v_valid_from, v_valid_until, v_is_active
    FROM coupons
    WHERE code = p_code
    LIMIT 1;

    IF v_discount_type IS NULL THEN
        SET p_error_msg = 'Kupon kodu geçerli değil';
    ELSEIF v_is_active = FALSE THEN
        SET p_error_msg = 'Bu kupon artık aktif değil';
    ELSEIF NOW() < v_valid_from THEN
        SET p_error_msg = 'Kuponun geçerlilik tarihi başlamamış';
    ELSEIF NOW() > v_valid_until THEN
        SET p_error_msg = 'Kuponun süresi dolmuş';
    ELSEIF v_max_uses IS NOT NULL AND v_used_count >= v_max_uses THEN
        SET p_error_msg = 'Kupon kullanım limiti doldu';
    ELSEIF v_min_order IS NOT NULL AND p_subtotal < v_min_order THEN
        SET p_error_msg = CONCAT('Bu kupon için minimum sipariş tutarı: ', v_min_order, '₺');
    ELSE
        -- İndirim hesapla
        IF v_discount_type = 'PERCENTAGE' THEN
            SET p_discount = ROUND(p_subtotal * (v_discount_value / 100), 2);
        ELSE
            SET p_discount = LEAST(v_discount_value, p_subtotal);
        END IF;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- EVENT: Her gece süresi dolmuş token temizliği
-- (MySQL Event Scheduler etkin olmalıdır: SET GLOBAL event_scheduler = ON;)
-- ============================================================
DROP EVENT IF EXISTS evt_nightly_token_cleanup;
CREATE EVENT evt_nightly_token_cleanup
    ON SCHEDULE EVERY 1 DAY
    STARTS CURRENT_TIMESTAMP + INTERVAL 1 HOUR
    DO CALL sp_cleanup_expired_tokens();
