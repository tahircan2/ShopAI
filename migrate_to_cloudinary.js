/**
 * ============================================================
 * CLOUDINARY MIGRATION SCRIPT (MySQL Version)
 * ============================================================
 * Unsplash URL'lerini Cloudinary'e upload eder ve
 * veritabanındaki image_url'leri günceller.
 * ============================================================
 */

import { v2 as cloudinary } from "cloudinary";
import mysql from "mysql2/promise";
import dotenv from "dotenv";
import fs from "fs";
import path from "path";

// .env dosyasını hem root'tan hem de backend'den dene
dotenv.config();
dotenv.config({ path: path.resolve(process.cwd(), "backend", ".env") });

// Cloudinary yapılandırması
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// MySQL bağlantısı
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USERNAME || "shopai_user",
  password: process.env.DB_PASSWORD,
  database: "shopai",
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

// Ürün → Unsplash Mapping
const PRODUCT_IMAGE_MAP = [
  // Legacy Products (1-30)
  { productId: 1, sku: "APL-IP15P-256", url: "https://images.unsplash.com/photo-1696446701796-da61225697cc?w=800&h=600&fit=crop" },
  { productId: 2, sku: "SAM-S24U-512", url: "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&h=600&fit=crop" },
  { productId: 3, sku: "APL-MBA-M3-512", url: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&h=600&fit=crop" },
  { productId: 4, sku: "SNY-WH1000XM5", url: "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&h=600&fit=crop" },
  { productId: 5, sku: "NK-DRFT-TST", url: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=600&fit=crop" },
  { productId: 6, sku: "ADD-ESS-TST", url: "https://images.unsplash.com/photo-1586790170083-2f9ceadc732d?w=800&h=600&fit=crop" },
  { productId: 7, sku: "NK-AM270-KAY", url: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=600&fit=crop" },
  { productId: 8, sku: "ADD-UB23-KAY", url: "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&h=600&fit=crop" },
  { productId: 9, sku: "CNV-CT-HI", url: "https://images.unsplash.com/photo-1605333396915-47ed6b68a00e?w=800&h=600&fit=crop" },
  { productId: 11, sku: "SAM-GW6CLS-47", url: "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=800&h=600&fit=crop" },
  { productId: 12, sku: "APL-APP2-USBC", url: "https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?w=800&h=600&fit=crop" },
  { productId: 13, sku: "LNV-IPS5-I7-16", url: "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&h=600&fit=crop" },
  { productId: 14, sku: "ASUS-ROG-G16", url: "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800&h=600&fit=crop" },
  { productId: 15, sku: "XMI-RDMPADSE", url: "https://images.unsplash.com/photo-1589739900243-4b52cd9b104e?w=800&h=600&fit=crop" },
  { productId: 18, sku: "NB-574-UNI", url: "https://images.unsplash.com/photo-1539185441755-769473a23570?w=800&h=600&fit=crop" },
  { productId: 19, sku: "PUM-RSX-EFT-UNI", url: "https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?w=800&h=600&fit=crop" },
  { productId: 20, sku: "MAV-SLIMFIT-MRK", url: "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800&h=600&fit=crop" },
  { productId: 21, sku: "LVS-501-ORG-STR", url: "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800&h=600&fit=crop" },
  { productId: 22, sku: "TH-ESS-SLIM-TSH", url: "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=800&h=600&fit=crop" },
  { productId: 23, sku: "ZAR-OVR-BSK-UNI", url: "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=800&h=600&fit=crop" },
  { productId: 24, sku: "CHP-RVW-HOOD-UNI", url: "https://images.unsplash.com/photo-1578587018452-892bacefd3f2?w=800&h=600&fit=crop" },
  { productId: 25, sku: "NK-TECHFLC-ZIP", url: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=600&fit=crop" },
  { productId: 26, sku: "RB-3025-AVTR", url: "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=600&fit=crop" },
  { productId: 27, sku: "SAM-PRXS-BKP-BLK", url: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=600&fit=crop" },
  { productId: 28, sku: "SNY-WH-SLV", url: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=600&fit=crop" },
  { productId: 29, sku: "NK-AM270-VOLT", url: "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=800&h=600&fit=crop" },
  { productId: 30, sku: "ADD-UB-LIGHT-V9", url: "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&h=600&fit=crop" },

  // New Era Products (35-84) - Reference Only
  { productId: 35, sku: "SAM-PHN-S24U", url: "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&h=600&fit=crop" },
  { productId: 36, sku: "APL-PHN-IP15P", url: "https://images.unsplash.com/photo-1696446701796-da61225697cc?w=800&h=600&fit=crop" }
  // ... (Full list from previous run abbreviated for migration focus)
];

const delay = (ms) => new Promise((r) => setTimeout(r, ms));

async function uploadToCloudinary(item) {
  const publicId = `${item.sku.toLowerCase()}`;
  try {
    const result = await cloudinary.uploader.upload(item.url, {
      public_id: publicId,
      folder: "products",
      overwrite: true,
      resource_type: "image",
      transformation: [
        { width: 800, height: 600, crop: "fill", gravity: "auto" },
        { quality: "auto:good" },
        { fetch_format: "auto" },
      ],
      tags: ["seed", "legacy", "migration"],
    });

    console.log(`  ✅ [${item.productId}] ${item.sku} → ${result.secure_url}`);
    return { ...item, cloudinaryUrl: result.secure_url };
  } catch (err) {
    console.error(`  ❌ [${item.productId}] ${item.sku} FAILED: ${err.message}`);
    return { ...item, cloudinaryUrl: null, error: err.message };
  }
}

async function updateDatabase(conn, productId, cloudinaryUrl) {
  // Hem primary image'ı güncelle hem de eğer yoksa oluştur
  const [rows] = await conn.execute(
    `SELECT id FROM product_images WHERE product_id = ? AND is_primary = TRUE`,
    [productId]
  );

  if (rows.length > 0) {
    await conn.execute(
      `UPDATE product_images SET image_url = ? WHERE product_id = ? AND is_primary = TRUE`,
      [cloudinaryUrl, productId]
    );
  } else {
    await conn.execute(
      `INSERT INTO product_images (product_id, image_url, is_primary, alt_text, sort_order) 
       VALUES (?, ?, TRUE, 'Product Image', 0)`,
      [productId, cloudinaryUrl]
    );
  }
}

async function migrate() {
  console.log("🚀 Legacy Product Image Migration Başladı (MySQL)");
  const results = { success: [], failed: [] };
  let conn;

  try {
    conn = await pool.getConnection();
    await conn.beginTransaction();

    for (let i = 0; i < PRODUCT_IMAGE_MAP.length; i++) {
      const item = PRODUCT_IMAGE_MAP[i];
      // Sadece 1-30 arasını işle (Yenileri zaten yaptık)
      if (item.productId > 30) continue;

      console.log(`[${i + 1}/${30}] İşleniyor: ${item.sku}`);
      const uploaded = await uploadToCloudinary(item);

      if (uploaded.cloudinaryUrl) {
        await updateDatabase(conn, item.productId, uploaded.cloudinaryUrl);
        results.success.push(uploaded);
      } else {
        results.failed.push(uploaded);
      }

      if ((i + 1) % 5 === 0) await delay(500);
    }

    await conn.commit();
    console.log("\n✅ Tüm legacy görseller güncellendi.");
  } catch (err) {
    if (conn) await conn.rollback();
    console.error("\n❌ HATA:", err.message);
  } finally {
    if (conn) conn.release();
    await pool.end();
  }
  
  console.log(`📊 Sonuç: ${results.success.length} Başarılı, ${results.failed.length} Başarısız`);
}

migrate();
