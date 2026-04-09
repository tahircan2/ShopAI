-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V4: Indexes — Performance + FULLTEXT (AI arama için)
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- users — sık kullanılan sorgular
-- ============================================================
-- Email ile login sorgusu
CREATE INDEX idx_users_email ON users (email);
-- Kilitli hesap kontrolü
CREATE INDEX idx_users_locked_until ON users (locked_until);
-- E-posta doğrulama token araması
CREATE INDEX idx_users_email_verify_token ON users (email_verify_token);
-- Şifre sıfırlama token araması
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token);

-- ============================================================
-- refresh_tokens — token rotation ve cleanup
-- ============================================================
-- Token hash ile hızlı doğrulama
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
-- Süresi dolmuş token'ları temizleme (TokenCleanupScheduler)
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
-- İptal edilmemiş aktif token'lar
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens (revoked_at);

-- ============================================================
-- categories — kategori ağacı sorgular
-- ============================================================
CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_categories_slug ON categories (slug);
CREATE INDEX idx_categories_active ON categories (is_active);

-- ============================================================
-- products — liste, filtre, arama sorgular (en kritik tablo)
-- ============================================================
-- Kategori bazlı listeleme
CREATE INDEX idx_products_category ON products (category_id);
-- Aktif ürünler filtresi
CREATE INDEX idx_products_active ON products (is_active);
-- Öne çıkan ürünler
CREATE INDEX idx_products_featured ON products (is_featured);
-- Fiyat aralığı filtresi
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_discounted_price ON products (discounted_price);
-- Marka filtresi (AI Filter Agent kullanır)
CREATE INDEX idx_products_brand ON products (brand);
-- Puan sıralama
CREATE INDEX idx_products_rating ON products (rating_avg DESC, rating_count DESC);
-- Composite: kategori + aktif + fiyat (en yaygın filtre kombinasyonu)
CREATE INDEX idx_products_category_active_price ON products (category_id, is_active, price);
-- Tarih bazlı sıralama (yeni ürünler)
CREATE INDEX idx_products_created_at ON products (created_at DESC);

-- FULLTEXT: Doğal dil araması — hem ProductController hem AI Filter Agent kullanır
-- NOT: FULLTEXT index InnoDB'de MySQL 5.6+ desteklenir
ALTER TABLE products
    ADD FULLTEXT INDEX ft_products_search (name, description, brand)
    WITH PARSER ngram;

-- ============================================================
-- product_images
-- ============================================================
CREATE INDEX idx_product_images_product ON product_images (product_id);
-- Ana görseli hızlı bulmak için
CREATE INDEX idx_product_images_primary ON product_images (product_id, is_primary);

-- ============================================================
-- product_variants
-- ============================================================
CREATE INDEX idx_product_variants_product ON product_variants (product_id);
-- Renk/beden filtreleri (AI Filter Agent)
CREATE INDEX idx_product_variants_color ON product_variants (color);
CREATE INDEX idx_product_variants_size ON product_variants (size);

-- ============================================================
-- reviews
-- ============================================================
CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);
-- Onaylı yorumları filtrelemek için
CREATE INDEX idx_reviews_approved ON reviews (product_id, is_approved);
CREATE INDEX idx_reviews_created ON reviews (created_at DESC);

-- ============================================================
-- carts & cart_items
-- ============================================================
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product ON cart_items (product_id);

-- ============================================================
-- address
-- ============================================================
CREATE INDEX idx_address_user ON address (user_id);
-- Varsayılan adres hızlı sorgusu
CREATE INDEX idx_address_default ON address (user_id, is_default);

-- ============================================================
-- coupons
-- ============================================================
CREATE INDEX idx_coupons_code ON coupons (code);
-- Geçerli kupon kontrolü (tarih + aktiflik)
CREATE INDEX idx_coupons_validity ON coupons (is_active, valid_from, valid_until);

-- ============================================================
-- orders
-- ============================================================
CREATE INDEX idx_orders_user ON orders (user_id);
-- Kullanıcı sipariş geçmişi (tarih sıralı)
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
-- Admin sipariş yönetimi filtreleri
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_payment_status ON orders (payment_status);
CREATE INDEX idx_orders_created ON orders (created_at DESC);

-- ============================================================
-- order_items
-- ============================================================
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- ============================================================
-- ai_conversations
-- ============================================================
CREATE INDEX idx_ai_conversations_user ON ai_conversations (user_id);
CREATE INDEX idx_ai_conversations_session ON ai_conversations (session_id);
CREATE INDEX idx_ai_conversations_last_msg ON ai_conversations (last_message_at DESC);

-- ============================================================
-- ai_messages
-- ============================================================
CREATE INDEX idx_ai_messages_conversation ON ai_messages (conversation_id);
-- Injection tespiti istatistikleri için
CREATE INDEX idx_ai_messages_injection ON ai_messages (is_injection_detected);
CREATE INDEX idx_ai_messages_created ON ai_messages (created_at DESC);

-- ============================================================
-- audit_logs — güvenlik izleme sorgular
-- ============================================================
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
-- Entity bazlı sorgular
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
-- Zaman bazlı güvenlik izleme
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
-- IP bazlı brute force tespiti
CREATE INDEX idx_audit_logs_ip ON audit_logs (ip_address, created_at DESC);

-- ============================================================
-- user_sessions
-- ============================================================
CREATE INDEX idx_user_sessions_user ON user_sessions (user_id);
-- Süresi dolmuş session cleanup
CREATE INDEX idx_user_sessions_expires ON user_sessions (expires_at);
CREATE INDEX idx_user_sessions_active ON user_sessions (user_id, is_active);

-- ============================================================
-- wishlist_items
-- ============================================================
CREATE INDEX idx_wishlist_user ON wishlist_items (user_id);
CREATE INDEX idx_wishlist_product ON wishlist_items (product_id);

-- ============================================================
-- notifications
-- ============================================================
CREATE INDEX idx_notifications_user ON notifications (user_id);
-- Okunmamış bildirimler (badge count)
CREATE INDEX idx_notifications_unread ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications (created_at DESC);
