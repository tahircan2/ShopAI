-- V14__remove_conflicting_triggers.sql
-- V11'de Stok Trigger'ları temizlenmişti. 
-- Bu dosya kalan diğer çifte işlemlere yol açan (Kupon ve Yorumlar) trigger'ları temizler.

-- Kupon kullanıldığında sayacı artıran trigger. 
-- OrderService.java içindeki `incrementUsedCount` ile çakışıp çifte kupon kullanımına yol açıyordu.
DROP TRIGGER IF EXISTS trg_orders_coupon_after_insert;

-- Yorum eklendiğinde/silindiğinde ortalama reyting hesaplayan triggerlar. 
-- ProductService.java içindeki `updateProductRating` metodu ile çakışıp veritabanı performans
-- ve transaction lock sorunlarına yol açma riski barındırıyordu.
DROP TRIGGER IF EXISTS trg_reviews_after_insert;
DROP TRIGGER IF EXISTS trg_reviews_after_update;
DROP TRIGGER IF EXISTS trg_reviews_after_delete;
