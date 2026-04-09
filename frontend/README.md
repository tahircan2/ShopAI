# ShopAI Angular Frontend — v3.0

Angular 19 Standalone, Signals, Dark Theme, HttpOnly Cookie JWT

## 🚀 Hızlı Başlangıç

```bash
npm install
ng serve
```

Uygulama `http://localhost:4200` adresinde çalışır.
Backend API: `http://localhost:8080/api` (proxy.conf.json ile otomatik yönlendirme)

## 📁 Proje Yapısı

```
src/app/
├── core/
│   ├── models/          ← TypeScript interface'leri (user, product, cart, order, chat)
│   ├── services/        ← auth, cart, product, order, user, ai-chat, notification, wishlist
│   ├── interceptors/    ← auth (withCredentials + refresh), csrf, loading
│   └── guards/          ← authGuard, adminGuard, sellerGuard, guestGuard
├── features/
│   ├── auth/            ← login, register, forgot-password, reset-password, verify-email
│   ├── home/            ← Hero, featured products, features section
│   ├── products/        ← product-list (filtreler+sayfalama), product-detail (galeri+yorumlar)
│   ├── cart/            ← Sepet, kupon, özet
│   ├── checkout/        ← 3 adımlı wizard (adres → özet → ödeme)
│   ├── orders/          ← Sipariş listesi + iptal
│   ├── profile/         ← 4 tab (profil/siparişler/adresler/güvenlik)
│   ├── wishlist/        ← Favori listesi
│   ├── admin/           ← Dashboard, kullanıcılar, ürünler, siparişler, kategoriler, kuponlar, audit
│   └── seller/          ← Dashboard, ürünlerim, siparişler, mağaza ayarları
└── shared/
    ├── components/      ← navbar, footer, toast, session-expired, chatbot, loading-bar, confirm-dialog
    ├── pipes/           ← currencyFormat, timeAgo, truncate
    └── directives/      ← clickOutside, debounceClick, lazyLoadImage
```

## 🔐 Güvenlik Mimarisi (v3)

| Özellik | Uygulama |
|---------|----------|
| Token storage | HttpOnly cookie — localStorage KULLANILMAZ |
| CSRF koruması | SameSite=Strict + X-XSRF-TOKEN header |
| Token yenileme | Refresh token rotation — sonsuz döngü koruması |
| Session expired | 5 saniyelik sayaçlı yönlendirme bileşeni |
| Prompt injection | 3 katmanlı: Frontend regex + Spring sanitize + Python LLM |
| API key güvenliği | environment.ts'e API key ASLA koyulmaz |

## 🎨 Design System

CSS Variables (`src/styles.scss`):

```scss
--clr-bg: #0a0a0f           // Ana arka plan
--clr-surface: #111118       // Kart yüzeyleri
--clr-primary: #7c6ff7       // Ana renk (mor)
--clr-accent: #f0a060        // Vurgu rengi (turuncu)
--clr-success / danger / warning / info
--font-sans: 'DM Sans'
--font-mono: 'Space Mono'
```

## 👥 Roller

| Rol | Erişim |
|-----|--------|
| USER | Profil, sepet, sipariş, favori |
| SELLER | + Satıcı paneli (ürün CRUD, sipariş takibi, mağaza ayarları) |
| ADMIN | + Admin paneli (kullanıcı yönetimi, tüm siparişler, kategoriler, kuponlar, audit log) |

## 🤖 AI Chatbot

Sağ alt köşedeki chatbot widget'ı:
- Doğal dil → ürün filtresi (`kırmızı nike 500₺ altı`)
- Sepete AI üzerinden ekleme
- Sipariş sorgulama
- SSS yanıtlama
- 3 katmanlı prompt injection koruması

## 📦 Bağımlılıklar

- Angular 19 (Standalone Components)
- Angular Signals (state management)
- Angular Router (lazy loading, view transitions)
- Angular Forms (reactive forms)
- RxJS 7.8

## 🔧 Geliştirme Notları

1. **withCredentials**: Tüm HTTP isteklerine otomatik eklenir (`auth.interceptor.ts`)
2. **Refresh token**: 401 alındığında otomatik token yenileme, başarısızsa session expired
3. **Cart badge**: `CartService.cartCount` computed signal — gerçek zamanlı güncelleme
4. **AI filter**: `ProductService.aiFilteredProducts` signal ile product-list otomatik güncellenir

## 🏗️ Backend Bağlantısı

```
Spring Boot: http://localhost:8080
Angular Dev: http://localhost:4200
Proxy: /api → http://localhost:8080/api (proxy.conf.json)

Python AI: http://localhost:8000
AI çağrıları Angular → Spring Boot → Python şeklinde proxy edilir
```
