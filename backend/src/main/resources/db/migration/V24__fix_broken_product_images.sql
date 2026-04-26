-- V24: Fix broken image URLs for specific products

UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&q=80' WHERE product_id = 2;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800&q=80' WHERE product_id = 53;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&q=80' WHERE product_id = 55;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&q=80' WHERE product_id = 64;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1491553895911-0055eca6402d?w=800&q=80' WHERE product_id = 71;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=800&q=80' WHERE product_id = 95;
UPDATE product_images SET image_url = 'https://images.unsplash.com/photo-1578587018452-892bace94f12?w=800&q=80' WHERE product_id = 96;
