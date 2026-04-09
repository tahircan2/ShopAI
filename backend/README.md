# ShopAI Backend — v3.0 Security-Hardened Edition

Spring Boot 3 · Java 21 · MySQL 8 · JWT HttpOnly Cookie

---

## Hızlı Başlangıç

### 1. Gereksinimler
- Java 21+
- MySQL 8.0+
- Maven 3.9+

### 2. MySQL Kurulumu

```sql
CREATE DATABASE shopai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'shopai_user'@'localhost' IDENTIFIED BY 'Guclusifre123!';
GRANT ALL PRIVILEGES ON shopai.* TO 'shopai_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Ortam Değişkenleri

`.env` dosyası oluştur veya sistem ortam değişkenlerine ekle:

```bash
DB_PASSWORD=Guclusifre123!
JWT_SECRET=en-az-256-bit-uzunlugunda-rastgele-bir-secret-key-buraya-gelecek!!
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=gmail-app-password
AI_SERVICE_URL=http://localhost:8000
AI_INTERNAL_KEY=internal-servis-anahtari
FRONTEND_URL=http://localhost:4200
COOKIE_DOMAIN=localhost
```

### 4. Çalıştırma

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

Uygulama: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui

---

## Proje Yapısı

```
src/main/java/com/shopai/
├── config/          SecurityConfig, AsyncConfig, WebClientConfig, OpenApiConfig
├── controller/      AuthController, ProductController, CartController,
│                    OrderController, UserController, AdminController,
│                    AiController, NotificationController
├── service/         AuthService, ProductService, CartService, OrderService,
│                    UserService, AiService, NotificationService,
│                    CookieService, EmailService, AuditLogService
├── repository/      14 adet JPA Repository
├── entity/          19 adet Entity (User, Product, Order, Cart, ...)
├── dto/
│   ├── request/     AuthRequests, ProductRequests, CartRequests,
│   │                OrderRequests, UserRequests, AiRequests
│   └── response/    AuthResponses, ProductResponses, CartResponse,
│                    OrderResponses, AddressResponse
├── security/        JwtUtil, JwtAuthFilter, JwtAuthDetails,
│                    CustomUserDetailsService
├── exception/       GlobalExceptionHandler + 5 custom exception
└── scheduler/       TokenCleanupScheduler, OrderStatusScheduler
```

---

## Güvenlik Mimarisi

| Özellik | Detay |
|---------|-------|
| Token Storage | HttpOnly; Secure; SameSite=Strict Cookie |
| Token Süresi | Access: 15dk, Refresh: 7 gün |
| Refresh Rotation | Her kullanımda yeni token, eski iptal |
| Hesap Kilitleme | 5 başarısız giriş → 15dk kilit |
| CSRF Koruması | SameSite=Strict + CSRF Token |
| userId Kaynağı | Her zaman JWT'den — body/param'dan asla |
| AI userId | X-Authenticated-User-Id header — mesajdan asla |
| Şifre Hash | BCrypt (strength=12) |
| Audit Log | Tüm kritik işlemler async kaydedilir |

---

## Endpoint Özeti

### Auth — `/api/auth`
| Method | Path | Açıklama |
|--------|------|----------|
| POST | /register | Kayıt |
| POST | /login | Giriş (cookie set) |
| POST | /refresh | Token yenile (cookie rotation) |
| POST | /logout | Çıkış (cookie clear) |
| GET  | /me | Oturum durumu |
| POST | /forgot-password | Şifre sıfırlama e-postası |
| POST | /reset-password | Yeni şifre belirle |
| GET  | /verify-email | E-posta doğrula |

### Products — `/api/products`
| Method | Path | Auth |
|--------|------|------|
| GET | /api/products | Public |
| GET | /api/products/search?q= | Public |
| GET | /api/products/featured | Public |
| GET | /api/products/{id} | Public |
| GET | /api/products/slug/{slug} | Public |
| GET | /api/products/{id}/reviews | Public |
| POST | /api/products/{id}/reviews | User |
| GET | /api/categories | Public |
| POST | /api/admin/products | Admin |
| PUT | /api/admin/products/{id} | Admin |
| DELETE | /api/admin/products/{id} | Admin |

### Cart — `/api/cart`
| Method | Path | Auth |
|--------|------|------|
| GET/POST/DELETE | /api/cart | User |
| POST/PUT/DELETE | /api/cart/items/{id} | User |
| POST/DELETE | /api/cart/coupon | User |

### Orders
| Method | Path | Auth |
|--------|------|------|
| POST | /api/orders | User |
| GET | /api/orders/{orderNumber} | User (ownership) |
| POST | /api/orders/{orderNumber}/cancel | User (ownership) |
| GET | /api/users/me/orders | User |
| GET | /api/admin/orders | Admin |
| PUT | /api/admin/orders/{id}/status | Admin |

### User Profile — `/api/users/me`
| Method | Path |
|--------|------|
| GET/PUT | /api/users/me |
| PUT | /api/users/me/password |
| GET/POST | /api/users/me/addresses |
| PUT/DELETE | /api/users/me/addresses/{id} |
| GET | /api/users/me/wishlist |
| POST/DELETE | /api/users/me/wishlist/{productId} |
| GET | /api/users/me/notifications |
| PUT | /api/users/me/notifications/{id}/read |

### AI — `/api/ai`
| Method | Path | Auth |
|--------|------|------|
| POST | /api/ai/chat | Public+ |
| GET | /api/ai/conversations/{sessionId} | User (ownership) |
| DELETE | /api/ai/conversations/{sessionId} | User (ownership) |

---

## Production Checklist

- [ ] `app.cookie.secure=true` (HTTPS zorunlu)
- [ ] `spring.jpa.hibernate.ddl-auto=validate`
- [ ] `springdoc.swagger-ui.enabled=false`
- [ ] JWT_SECRET en az 256-bit rastgele değer
- [ ] MySQL şifresi environment variable'dan
- [ ] Actuator endpoint'leri kısıtlandı
- [ ] CORS sadece production domain'i
- [ ] AI_INTERNAL_KEY güçlü ve gizli
