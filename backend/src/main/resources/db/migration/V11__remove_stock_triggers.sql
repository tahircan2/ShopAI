-- ============================================================
-- ShopAI E-Commerce — MySQL 8 Schema Migration
-- V11: Remove manual stock triggers to avoid duplication with Java logic
-- ============================================================

-- Remove redundant stock decrement on item insertion
DROP TRIGGER IF EXISTS trg_order_items_after_insert;

-- Remove redundant stock restoration on order cancellation (handled in Java)
DROP TRIGGER IF EXISTS trg_orders_after_update;
