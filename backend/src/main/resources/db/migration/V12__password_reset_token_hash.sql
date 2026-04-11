-- V12: Şifre sıfırlama token'ını SHA-256 hash olarak saklamak için sütun değişikliği
-- Eski: password_reset_token (bcrypt hash, 255 char) → Yeni: password_reset_token_hash (SHA-256 hex, 64 char)
-- Neden: Eski yöntem tüm kullanıcı tablosunu RAM'e yükleyip bcrypt ile karşılaştırıyordu (O(n) * bcrypt cost).
--        Yeni yöntem SHA-256 hash'i indexed sorguylaO(1)'de bulur.

ALTER TABLE users
    ADD COLUMN password_reset_token_hash VARCHAR(64) NULL AFTER email_verify_token,
    DROP INDEX idx_users_password_reset_token,
    ADD INDEX idx_users_password_reset_token_hash (password_reset_token_hash);

-- Eski sütun drop — yeni sistem SHA-256 hash kullanır
ALTER TABLE users
    DROP COLUMN password_reset_token;
