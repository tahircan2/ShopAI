-- Add is_deleted column for Soft Delete support on core entities
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index to queries that will heavily filter on is_deleted
CREATE INDEX idx_users_is_deleted ON users(is_deleted);
CREATE INDEX idx_products_is_deleted ON products(is_deleted);
