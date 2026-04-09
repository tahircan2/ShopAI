# GÖREV TANIMI

Sen kıdemli bir Full-Stack Engineer ve Senior UI/UX Developer olarak görev yapıyorsun. Aşağıdaki proje üzerinde listelenen tüm görevleri, en yüksek yazılım mühendisliği standartlarına uygun biçimde, eksiksiz olarak tamamlayacaksın.

## PROJE BAĞLAMI

**Proje:** ShopAI E-Commerce
**Stack:** Angular 19 (Standalone Components, OnPush CD, inject() pattern) • Spring Boot 3 (Java 21, JPA/Hibernate, JWT via HttpOnly Cookie, Spring Security) • MySQL 8 • LangGraph/Python FastAPI
**Güvenlik Mimarisi:** JWT token'lar localStorage'da değil, HttpOnly; Secure; SameSite=Strict cookie'lerde taşınır. Angular hiçbir zaman token string'ini görmez. withCredentials: true zorunludur. userId her zaman SecurityContextHolder'dan alınır, parametreden asla kabul edilmez.

Değişiklik yapmadan önce ilgili mevcut dosyayı oku ve analiz et. Sonra sadece değişen dosyaları ver. Değişmeyen dosyaları tekrar yazma. Olası her edge case, hata senaryosu ve çakışma için önlem al. Tüm çıktılar production-ready kalitede olmalı.

---

## GÖREV LİSTESİ

### GÖREV 1 — Wishlist Sayfası: Ürün Kartlarına "Sepete Ekle" Butonu Eklenmesi

**Kapsam:** `wishlist.component.ts`, `wishlist.component.html`, `wishlist.component.scss`, `cart.service.ts`

**Gereksinimler:**

1.1. Her wishlist ürün kartının üzerine, diğer ürün kartlarıyla (product-list, product-detail) tamamen tutarlı tasarımda bir "Sepete Ekle" butonu ekle.

1.2. **Beden (Size) Zorunluluğu:**
- Ürünün `variants` dizisinde birden fazla farklı `size` değeri varsa (ör. S, M, L, XL), kullanıcıya bir beden seçim modal'ı veya inline dropdown göster.
- Kullanıcı beden seçmeden "Sepete Ekle" butonuna basarsa işlem engellenmeli, bir toast veya inline uyarı ile "Lütfen bir beden seçiniz" mesajı gösterilmeli.
- Tüm varyantların `size` alanı null veya boşsa beden seçimi isteme, doğrudan devam et.

1.3. **Renk (Color) Otomatik Seçimi:**
- Ürünün varyantları arasında renk seçeneği varsa, sayfa yüklendiğinde `variants[0].color` otomatik olarak seçili gösterilmeli.
- Kullanıcı rengi değiştirmezse ilk renk ile devam edilmeli.
- Renk seçimi için küçük renkli daire butonlar (hex kodundan dinamik arka plan rengi) kullanılmalı, seçili renk görsel olarak vurgulanmalı (border veya check işareti).
- Renk yoksa renk seçimi gösterme.

1.4. **Stok Kontrolü:**
- Seçilen varyantın `stockQuantity === 0` ise buton "Stok Tükendi" olarak gösterilmeli ve disabled olmalı.
- Genel `stockQuantity === 0` (varyantsız ürünler için) da aynı kural geçerli.

1.5. Sepete ekleme başarılıysa: `CartService.addToCart()` çağrılmalı, başarı toast'ı gösterilmeli, `cart$` güncellemeli.

1.6. Loading durumunda buton spinner göstermeli ve çift tıklamayı önlemek için disabled olmalı.

**Olası Sorunlar ve Önlemler:**
- Wishlist servisi cart servisini inject etmiyorsa bağımlılığı ekle.
- Varyant verisi wishlist endpoint'inde gelmiyorsa backend `WishlistResponse` DTO'suna `variants` ve `stockQuantity` alanlarını ekle ve `WishlistService` sorgusu `JOIN FETCH` ile varyantları getirmeli.
- Ürün kartı tasarımı product-list kartından farklıysa tutarlı hale getir.

---

### GÖREV 2 — Checkout Akışı: Adres Seçimi (Adım 1) Düzeltmesi

**Kapsam:** `checkout.component.ts`, `checkout.component.html`, `checkout.component.scss`, `address.service.ts` (veya `user.service.ts`), `AddressController.java`, ilgili DTO'lar

**Gereksinimler:**

2.1. **Varsayılan Adres Otomatik Seçimi:**
- Checkout Adım 1 yüklendiğinde `GET /api/users/me/addresses` çağrılmalı.
- Dönen adresler arasında `isDefault: true` olan varsa otomatik olarak seçili (radio/highlighted) gösterilmeli.
- Kullanıcı seçimi değiştirmezse bu adresle devam edilmeli.

2.2. **Adres Yoksa Konum Ekleme Zorunluluğu:**
- Kullanıcının hiç adresi yoksa Adım 1'de inline bir "Adres Ekle" formu gösterilmeli (ayrı sayfaya yönlendirme yapma, checkout akışını bozma).
- Bu form doldurulup kaydedilmeden "İleri" butonu disabled olmalı.
- Adres kaydedildikten sonra form kaybolmalı, yeni adres otomatik seçili gelip akış devam etmeli.

2.3. **Birden Fazla Adres — Seçim Zorunluluğu:**
- Birden fazla adres varsa ve kullanıcı hiçbirini seçmemişse (default da yoksa) "İleri" butonu disabled olmalı.
- Butona basıldığında "Lütfen bir teslimat adresi seçiniz" uyarısı gösterilmeli.
- Adres listesi radio button veya seçili kartlar şeklinde gösterilmeli; aynı anda maksimum 1 adres seçilebilmeli (bu zaten radio mantığı).

2.4. **Adres Ekleme Formu (Inline):**
- `address_line1`, `city`, `district`, `postal_code`, `full_name`, `phone` alanlarını içermeli.
- Tüm zorunlu alanlar için Angular reactive form validasyonu yapılmalı.
- Kaydedilince `POST /api/users/me/addresses` çağrılmalı, başarılıysa adres listesi yenilenmeli.

2.5. `userId` hiçbir form alanına veya API isteğinin body/param kısmına eklenmemeli; backend JWT'den alır.

**Olası Sorunlar ve Önlemler:**
- Checkout stepper'ın "İleri" butonu merkezi bir `canProceed()` metoduyla kontrol edilmeli; her adımın kendi validation state'i burada değerlendirilmeli.
- Adres formu kaydedilirken loading state yönetilmeli, çift submit önlenmeli.
- Backend'de `AddressResponse` DTO'sunda `isDefault` alanı eksikse ekle.

---

### GÖREV 3 — Sipariş Tamamlama Sonrası Sayfa (Order Confirmation Page)

**Kapsam:** Yeni `order-confirmation.component.ts/html/scss`, `app.routes.ts`, `OrderService`, `OrderController.java` (gerekiyorsa), `order.model.ts`

**Gereksinimler:**

3.1. **Yönlendirme Düzeltmesi:**
- Sipariş başarıyla oluşturulduktan sonra 404 sayfasına gidilmemeli.
- `OrderService.createOrder()` başarılı response aldıktan sonra `Router.navigate(['/orders/confirmation', orderNumber])` ile yeni sayfaya gidilmeli.
- Route: `/orders/confirmation/:orderNumber` — auth guard ile korunmalı.

3.2. **Order Confirmation Sayfası İçeriği:**
Aşağıdaki bölümlerin hepsi eksiksiz ve görsel olarak güzel olmalı:

**a) Başarı Header Bölümü:**
- Büyük animasyonlu yeşil checkmark (CSS/SVG animasyonu, kütüphane kullanma).
- "Siparişiniz Başarıyla Oluşturuldu!" başlığı.
- "Teşekkürler [firstName]! Siparişiniz alındı." alt metni.

**b) Sipariş Özet Kartı:**
- Sipariş numarası (ör. ORD-20240101-XXXX) — kopyala butonu ile.
- Sipariş tarihi ve saati.
- Tahmini teslimat tarihi (sipariş tarihinden +3-5 iş günü).
- Ödeme yöntemi ve ödeme durumu.
- Toplam tutar (KDV dahil, formatlanmış).

**c) Sipariş Edilen Ürünler:**
- Her ürün için: görsel (thumbnail), ürün adı, varyant (renk/beden), adet, birim fiyat, toplam fiyat.
- Alt toplam, kargo ücreti, KDV, indirim (varsa), genel toplam.

**d) Teslimat Adresi:**
- Seçilen adresin tüm alanları gösterilmeli.

**e) Kargo Takip Bölümü:**
- Sipariş durumu timeline'ı: PENDING → CONFIRMED → SHIPPED → DELIVERED (şu anki adım vurgulanmış).
- Her adım için icon ve tarih/saat (gerçekleşmemiş adımlar soluk/disabled görünmeli).
- "Kargo takip numarası mevcut olduğunda buraya eklenecektir." placeholder metni (shipped durumunda gerçek tracking no gösterilmeli).

**f) Aksiyon Butonları:**
- "Siparişlerimi Görüntüle" → `/orders` sayfasına git.
- "Alışverişe Devam Et" → `/products` sayfasına git.
- "Ana Sayfaya Dön" → `/` ana sayfaya git.

3.3. **Veri Yükleme:**
- `GET /api/orders/:orderNumber` endpoint'i çağrılarak gerçek sipariş verisi gösterilmeli.
- Loading skeleton gösterilmeli, hata durumunda retry mekanizması olmalı.
- Ownership check backend'de zorunlu (başkasının siparişi görüntülenemez).

3.4. **Sayfa Yenileme Koruması:**
- Sayfa yenilendiğinde orderNumber route param'dan alınarak API'den veri yeniden çekilmeli; veri kaybolmamalı.

**Olası Sorunlar ve Önlemler:**
- `orderNumber` route param üzerinden geçirilecek, state ile değil (state sayfa yenilemede kaybolur).
- Backend `GET /api/orders/{orderNumber}` yoksa `GET /api/users/me/orders/{orderNumber}` endpoint'i oluştur ve ownership check ekle.

---

### GÖREV 4 — Fiyat Pipe'ı: Türk Lirası Formatı

**Kapsam:** `currency-format.pipe.ts`, fiyat gösterilen tüm component template'leri

**Gereksinimler:**

4.1. `CurrencyFormatPipe` oluştur veya mevcut pipe'ı düzelt:
```typescript
// Girdi: 123456 → Çıktı: ₺123.456,00
// Girdi: 1500.5 → Çıktı: ₺1.500,50
// Girdi: 0 → Çıktı: ₺0,00
// Girdi: null/undefined → Çıktı: '—'
```

4.2. Türkçe standart format:
- Binlik ayraç: nokta (`.`)
- Ondalık ayraç: virgül (`,`)
- Her zaman 2 ondalık hane
- ₺ sembolü sayının önünde, aralarında boşluk yok
- Angular'ın `CurrencyPipe`'ını kullan: `{{ value | currency:'TRY':'symbol-narrow':'1.2-2':'tr-TR' }}`
- Eğer bu format doğru çalışmıyorsa (ki tr-TR locale Angular'da kayıtlı olmayabilir), custom pipe yaz.

4.3. Custom pipe örneği (locale sorunu varsa):
```typescript
transform(value: number | null | undefined): string {
  if (value == null) return '—';
  return '₺' + value.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
```

4.4. Fiyat gösterilen tüm yerlerde bu pipe kullanılmalı:
- `product-list`, `product-detail`, `cart`, `checkout`, `order-confirmation`, `order-history`, `wishlist`, admin ürün tablosu.

4.5. `main.ts`'te `LOCALE_ID: 'tr-TR'` ve `registerLocaleData(localeTr)` ekle (Angular locale kaydı).

**Olası Sorunlar ve Önlemler:**
- `@angular/common/locales/tr` import'unu unutma.
- SSR kullanılıyorsa server-side locale de ayarlanmalı.
- Pipe standalone ise `imports` dizisine eklenmeli.

---

### GÖREV 5 — Arama Fonksiyonu: Tam Düzeltme ve UX İyileştirmesi

**Kapsam:** `header.component.ts/html/scss`, `product.service.ts`, `product-list.component.ts/html/scss`, `app.routes.ts`

**Gereksinimler:**

5.1. **Arama Mimarisi:**
- Header'daki arama inputuna kullanıcı yazar ve Enter'a basar veya arama ikonuna tıklar.
- `Router.navigate(['/products'], { queryParams: { q: searchTerm } })` ile yönlendirme yapılır.
- `ProductListComponent` ngOnInit'te `ActivatedRoute.queryParams` subscribe olur, `q` parametresi varsa `ProductService.searchProducts(q)` çağırır.
- Eğer `/api/products/search?q=` endpoint'i backend'de düzgün çalışmıyorsa kontrol et; MySQL FULLTEXT search veya LIKE sorgusu ile `ProductSpecification`'a entegre et.

5.2. **Arama Kutusunun Kalıcılığı:**
- Kullanıcı bir arama yaptıktan sonra `/products?q=ayakkabı` sayfasındayken arama inputu "ayakkabı" değerini göstermeli.
- `ProductListComponent` yüklendiğinde `queryParams.q` değeri varsa header'daki search input'una yazılmalı (bunun için `HeaderService` veya `SearchService` ile bir BehaviorSubject kullan).
- Kullanıcı inputu manuel temizleyene kadar bu değer kalmaya devam etmeli.

5.3. **ESC ile Kapatma Düzeltmesi:**
- Arama kutusu açıldığında (overlay, dropdown veya expanded input) ESC tuşu her durumda kapatmalı.
- Bunun için `@HostListener('document:keydown.escape')` kullan; arama kutusu açık olup olmadığını state değişkeni ile takip et.
- Input içine tıklanmadan da (sadece overlay açıksa) ESC çalışmalı — bu `@HostListener` document seviyesinde dinlediği için düzelir.
- Ek olarak: arama kutusunun dışına tıklandığında da kapanmalı (`ClickOutside` directive kullanılabilir).

5.4. **Debounce:**
- Arama inputu için 400ms debounce uygulanmalı (sadece "anlık arama" varsa; Enter ile aramada debounce gerekmez).
- `fromEvent` + `debounceTime(400)` + `distinctUntilChanged()` + `switchMap` zinciri kullanılmalı.

5.5. **Loading ve Empty State:**
- Arama yapılırken skeleton gösterilmeli.
- Sonuç bulunamazsa "'{q}' için sonuç bulunamadı. Farklı bir arama yapmayı deneyin." mesajı gösterilmeli.

**Olası Sorunlar ve Önlemler:**
- Header ve ProductList farklı component tree'lerinde olabilir; aralarında sinyal paylaşımı için `SearchStateService` (singleton, providedIn: 'root') oluştur.
- QueryParam değişikliği aynı route içinde olduğunda `ngOnInit` tekrar tetiklenmez; bunun için `ActivatedRoute.queryParams` ile subscribe kullan, `takeUntilDestroyed()` ile memory leak önle.
- Backend FULLTEXT index yoksa Faz 2, Adım 19'a göre `products.name` ve `description` üzerine `ALTER TABLE products ADD FULLTEXT idx_ft_products (name, description)` ekle.

---

### GÖREV 6 — Sepet Sayfası: "Sepeti Temizle" Butonunun Konumu

**Kapsam:** `cart.component.html`, `cart.component.scss`

**Gereksinimler:**

6.1. "Sepeti Temizle" (Clear Cart) butonu, ürün listesinin **altında değil üstünde** olmalı.
6.2. Konumu: sepet başlığının ("Sepetim" veya "Cart") hemen yanında veya altında, ürün listesinin üzerinde sağa yaslanmış.
6.3. Buton stili: tehlikeli/destructive aksiyon olduğu için kırmızı veya outlined tehlike stili (ör. `btn-danger-outline`).
6.4. Tıklandığında onay dialog'u gösterilmeli: "Sepetinizdeki tüm ürünler silinecek. Emin misiniz?" → Onayla / İptal.
6.5. Sepet boşsa buton görünmemeli (`*ngIf="cart && cart.items.length > 0"`).

---

### GÖREV 7 — Ürünler Sayfası: Filtreleme Sistemi Tam Revizyonu

**Kapsam:** `product-list.component.ts/html/scss`, `product-filter.component.ts/html/scss` (varsa veya oluştur), `product.service.ts`, `ProductController.java`, `ProductSpecification.java`

**Gereksinimler:**

7.1. **Filter Paneli Standartları:**
- Sol sidebar filter paneli aşağıdaki bölümleri içermeli (her biri accordion/collapsible):
  - **Kategori** — checkbox listesi, backend'den gelen kategoriler
  - **Fiyat Aralığı** — çift taraflı range slider veya iki input (min/max)
  - **Renk** — renkli daire checkbox'lar (hex kodundan arka plan rengi)
  - **Beden** — küçük kare/yuvarlak buton toggle checkbox'lar (S, M, L, XL vb.)
  - **Marka** — checkbox listesi
  - **Minimum Puan** — 1-5 yıldız seçimi (radio veya yıldız butonları)

7.2. **"Filtrele" Butonu ile Uygulama:**
- Kullanıcı filtre seçimlerini yapar ama henüz bir şey değişmez.
- "Filtrele" (veya "Uygula") butonuna bastığında seçimler `ProductService.getProducts(filter)` çağrısına iletilir ve ürün listesi güncellenir.
- Sidebar'ın alt kısmında "Filtrele" butonu (primary, tam genişlik) ve yanında "Temizle" butonu olmalı.

7.3. **Seçili Filtre Etiketleri (Filter Chips):**
- Ürün listesinin üstünde aktif filtreler chip olarak gösterilmeli (ör. "Renk: Kırmızı ×", "Max: 500₺ ×").
- Her chip'in × butonuna basılınca o filtre kaldırılmalı ve "Filtrele" butonuna gerek kalmadan anlık kaldırılmalı.
- "Tümünü Temizle" chip/linki tüm filtreleri sıfırlamalı.

7.4. **"X Ürün Bulundu" Metni:**
- Ürün listesinin üstünde `totalElements` değeri dinamik gösterilmeli.
- İlk yükleme, filtre uygulaması ve arama sonrası güncellenmelidir.
- Yükleme sırasında skeleton/placeholder gösterilmeli.

7.5. **Backend Kontrolü:**
- `ProductSpecification.java` Criteria API filtrelerinin hepsini desteklediğini doğrula: `category`, `minPrice`, `maxPrice`, `colors` (IN), `sizes` (IN), `brand`, `minRating`, `sortBy`, `sortDir`.
- Eksik parametre varsa `null` kontrolü ile güvenli şekilde ekle.
- `GET /api/products?category=...&minPrice=...` isteğini Postman/Bruno ile test et.

7.6. **Sıralama (Sort):**
- Ürün listesinin üstünde bir dropdown: "En Yeniler", "En Düşük Fiyat", "En Yüksek Fiyat", "En Çok Değerlendirilen", "En Yüksek Puan".
- Sıralama seçildiğinde anlık uygulanmalı (filtre butonuna basmadan).

**Olası Sorunlar ve Önlemler:**
- Renk ve beden değerleri backend'de string olarak saklanıyorsa frontend'de büyük/küçük harf normalizasyonu yap.
- Range slider için native HTML5 range input veya minimal bir custom CSS slider kullan, ağır kütüphane ekleme.
- Filtre state'i queryParam ile sync'lenebilir (URL paylaşılabilirliği için), bu opsiyonel ama güçlü bir UX özelliği.

---

### GÖREV 8 — Ürünler Sayfası: Sayfalama (Pagination)

**Kapsam:** `product-list.component.ts/html/scss`, yeni `pagination.component.ts/html/scss`

**Gereksinimler:**

8.1. Sayfa başına **9 ürün** gösterilmeli (backend'e `size=9` gönderilmeli).
8.2. Alt kısımda standart pagination bileşeni:
- ← Önceki | 1 | 2 | 3 | ... | 12 | Sonraki →
- Aktif sayfa vurgulanmalı.
- İlk ve son sayfada ilgili ok disabled olmalı.
- Çok sayfa varsa ellipsis (...) kullanılmalı (ör. 1 2 3 ... 10 11 12).
8.3. Sayfa değiştiğinde sayfanın en üstüne smooth scroll yapılmalı.
8.4. Sayfa numarası queryParam olarak URL'e yazılmalı (`/products?page=2`), böylece geri tuşu çalışır.
8.5. Backend `ProductPage` response'undaki `totalPages`, `totalElements`, `page`, `size` alanlarını kullan.
8.6. Toplam ürün < 9 ise pagination gösterilmemeli.

---

### GÖREV 9 — Ürünler Sayfası: UX/UI Dengesi ve Layout Revizyonu

**Kapsam:** `product-list.component.ts/html/scss`, global `styles.scss`, `app.component.scss`

**Gereksinimler:**

9.1. **Sticky Sidebar:**
- Sol filter sidebar'ı `position: sticky; top: [navbar yüksekliği]px;` ile sabit kalmalı, kullanıcı ürünleri scroll ederken filtreler de görünmeli.
- Sidebar yüksekliği viewport'u aşarsa kendi içinde `overflow-y: auto` ile scroll edilebilmeli.

9.2. **Grid Layout:**
- Ürün grid'i: desktop'ta 3 kolon (sidebar + 3 kart), tablet'te 2 kolon, mobilde 1 kolon.
- CSS Grid veya Flexbox ile `gap` tutarlı olmalı.
- Kart yükseklikleri tutarlı olmalı (`grid-auto-rows` veya flex ile).

9.3. **Mobil Filtre:**
- Mobilde sidebar gizlenmeli, yerine "Filtrele 🎚" butonu gösterilmeli.
- Butona tıklanınca drawer/bottom-sheet olarak açılmalı, overlay arka plan koyulaşmalı.
- Drawer içinde "Filtrele" ve "Kapat" butonları olmalı.

9.4. **Boş State:**
- Filtre veya arama sonucu 0 ürün gelirse boş state görsel: icon + "Ürün bulunamadı" + "Filtreleri Temizle" butonu.

9.5. **Loading State:**
- İlk yükleme ve filtre uygulamasında 9 adet skeleton kart gösterilmeli (shimmer animasyonlu).
- Skeleton kartlar gerçek kart boyutlarında ve layout'ında olmalı.

9.6. **Kart Tasarımı Standardizasyonu:**
- Tüm product kartları proje genelinde (`wishlist`, `home featured`, `product-list`, `search results`) aynı component'i kullanmalı: `ProductCardComponent` (standalone, reusable).
- Kart: görsel, ürün adı (max 2 satır, ellipsis), fiyat (formatlanmış pipe ile), puan (yıldız), "Sepete Ekle" butonu, wishlist icon toggle.

---

### GÖREV 10 — Navbar ve Genel Sayfa UX/UI Standardizasyonu

**Kapsam:** `header.component.ts/html/scss`, `footer.component.ts/html/scss`, `home.component.ts/html/scss`, tüm feature component'leri

**Gereksinimler:**

10.1. **Navbar Eklentileri:**
Mevcut navbara şu sekmeleri ekle (varsa atla, yoksa ekle):
- Anasayfa (`/`)
- Ürünler (`/products`)
- Kategoriler (`/products?category=...`) — hover ile dropdown kategori menüsü
- Kampanyalar / İndirimler (`/products?sort=discount`)
- Hakkımızda (`/about`)
- İletişim (`/contact`)
- Sağ taraf: Arama ikonu | Favori ikonu (badge ile) | Sepet ikonu (badge ile) | Kullanıcı menüsü (giriş yapılmışsa: Profilim, Siparişlerim, Çıkış Yap; yapılmamışsa: Giriş Yap, Kayıt Ol)

10.2. **Navbar Tasarım Standardı:**
- Sticky navbar (`position: sticky; top: 0; z-index: 1000`).
- Mobilde hamburger menu (drawer ile açılır).
- Aktif route için underline veya renk vurgusu.
- Scroll'da navbar'ın hafif background blur efekti (backdrop-filter).

10.3. **Hakkımızda Sayfası (`/about`):**
- Hero bölümü: şirket adı, kısa slogan.
- Vizyon & Misyon bölümü.
- Ekip kartları (3-4 placeholder kart).
- Neden Biz? özellikleri (ikon + başlık + açıklama).
- Istatistik sayaçları (ör. 10.000+ müşteri, 5.000+ ürün).

10.4. **İletişim Sayfası (`/contact`):**
- İletişim formu: ad, e-posta, konu, mesaj. Reactive form validasyonu.
- Yan panel: adres, telefon, e-posta, çalışma saatleri.
- Google Maps embed placeholder.
- Form submit → toast başarı/hata mesajı (backend endpoint gerekiyorsa stub/mock da olabilir).

10.5. **Ana Sayfa (`/`) UX İyileştirmeleri:**
- Hero slider/banner: en az 2 slide, CTA butonu, otomatik geçiş + manuel kontrol.
- Öne çıkan kategoriler: kategori ikonları veya görselleriyle horizontal scroll veya grid.
- Öne çıkan ürünler: `ProductCardComponent` ile 6-8 ürün, "Tümünü Gör" linki.
- Kampanya/indirim banner'ı.
- Müşteri yorumları bölümü (3-4 fake review kartı — placeholder).
- Newsletter signup bölümü (e-posta input + abone ol butonu, backend stub).

10.6. **404 Sayfası (`/not-found`):**
- Büyük "404" başlık.
- "Aradığınız sayfa bulunamadı." mesajı.
- "Anasayfaya Dön" ve "Ürünlere Gözat" butonları.
- `**` wildcard route bu sayfaya yönlendirmeli.

---

### GÖREV 11 — Siparişlerim Sayfası: Loading Sorunu ve Tam Düzeltme

**Kapsam:** `orders.component.ts/html/scss`, `order-detail.component.ts/html/scss`, `OrderController.java`, `OrderService.java`

**Gereksinimler:**

11.1. **Loading Sorunu Tespiti ve Çözümü:**
- `GET /api/users/me/orders` endpoint'i çağrıldığında loading spinner dönüyor ve sayfada takılıyor. Nedeni araştır:
  - HTTP çağrısı tamamlanmıyor mu? (Network tab'ında pending mi?)
  - Backend'den hata mı dönüyor? (500, 403, vb.)
  - `finalize()` veya `complete()` tetiklenmiyor mu?
  - `LoadingService.setLoading(false)` doğru çağrılıyor mu?
- LoadingInterceptor'da `finalize(() => loadingService.setLoading(false))` kullanılmalı — sadece `tap` değil.

11.2. **Backend Endpoint Kontrolü:**
- `GET /api/users/me/orders` endpoint'inin `userId`'yi `SecurityContextHolder`'dan aldığını doğrula.
- `LazyInitializationException` gibi Hibernate hataları varsa fetch stratejisini düzelt (JOIN FETCH veya @Transactional).
- Response DTO'sunda `orderItems`, `shippingAddress`, `paymentStatus` alanlarının dahil olduğunu doğrula.

11.3. **Siparişlerim Sayfası UI:**
- Sipariş yoksa: boş state görseli + "Henüz siparişiniz yok." + "Alışverişe Başla" butonu.
- Sipariş varsa: her sipariş için kart:
  - Sipariş numarası, sipariş tarihi
  - Sipariş durumu (renkli badge: PENDING sarı, CONFIRMED mavi, SHIPPED mor, DELIVERED yeşil, CANCELLED kırmızı)
  - Ürün görselleri (max 3 thumbnail, fazlası "+N" badge)
  - Toplam tutar
  - "Detayları Gör" butonu → `/orders/:orderNumber`

11.4. **Sipariş Detay Sayfası:**
- Görev 3'te tasarlanan Order Confirmation sayfasıyla içerik tutarlı olmalı (aynı component veya shared görünüm).
- Ownership check: backend `userId` JWT'den alır, başka kullanıcının siparişi 403 döner.

---

### GÖREV 13 — Footer: Tüm Linklerin Çalışması ve İçerik Doldurulması

**Kapsam:** `footer.component.ts/html/scss`, ilgili tüm route'lar

**Gereksinimler:**

13.1. **Footer Bölümleri:**
Footer 4 kolona ayrılmalı:
- **ShopAI** — logo, kısa açıklama, sosyal medya ikonları (Instagram, Twitter/X, Facebook, LinkedIn — ikonlar için SVG veya Font Awesome kullan).
- **Hızlı Bağlantılar** — Anasayfa, Ürünler, Kampanyalar, Hakkımızda, İletişim.
- **Müşteri Hizmetleri** — SSS, Kargo Bilgisi, İade & Değişim, Sipariş Takibi, Gizlilik Politikası, Kullanım Koşulları.
- **İletişim** — E-posta, telefon, adres, çalışma saatleri.

13.2. **Tüm Linklerin Kontrolü:**
- Her link `routerLink` veya `href` ile doğru URL'e bağlı olmalı.
- Placeholder olan hiçbir `href="#"` kalmamalı; eğer henüz sayfa yoksa `/` veya yakında gelecek sayfa için `[disabled]` stili ile gösterilmeli.
- SSS, Kargo Bilgisi, İade Politikası, Gizlilik Politikası, Kullanım Koşulları için statik sayfa route'ları oluştur (`/faq`, `/shipping`, `/returns`, `/privacy`, `/terms`).

13.3. **Statik Sayfalar:**
Aşağıdaki 5 statik sayfa oluşturulmalı (standalone component, lazy loaded):
- `/faq` — Sıkça Sorulan Sorular (accordion formatında 8-10 soru/cevap)
- `/shipping` — Kargo Bilgisi (süre, ücret tablosu, ücretsiz kargo şartı)
- `/returns` — İade & Değişim Politikası
- `/privacy` — Gizlilik Politikası (KVKK uyumlu, placeholder içerik)
- `/terms` — Kullanım Koşulları

13.4. **Sosyal Medya Linkleri:**
- `target="_blank" rel="noopener noreferrer"` ile açılmalı.
- Gerçek URL'ler yerine `#` kullanılabilir ama `target="_blank"` olmalı.

13.5. **Alt Footer:**
- Copyright: "© 2025 ShopAI. Tüm hakları saklıdır."
- KVKK ve kullanım koşulları linklerine kısa link.

13.6. **Responsive:**
- Mobilde 4 kolon tek kolona düşmeli veya 2x2 grid olmalı.

---

## GENEL UYGULAMA KURALLARI (TÜM GÖREVLER İÇİN GEÇERLİ)

**Güvenlik — Asla ihlal etme:**
- `userId` hiçbir zaman component'ten, form'dan veya URL param'dan alınmaz. Her zaman `SecurityContextHolder`'dan.
- `localStorage` veya `sessionStorage` kullanılmaz. Token yönetimi cookie'de.
- `withCredentials: true` tüm HTTP isteklerinde zorunlu.
- `environment.ts`'e secret, API key veya token eklenmez.

**Angular Kod Standartları:**
- Angular 19 standalone component mimarisi.
- `inject()` fonksiyon pattern'i — constructor injection değil.
- `OnPush` change detection stratejisi.
- `takeUntilDestroyed()` ile memory leak önleme.
- Functional guard ve interceptor pattern'i.
- `async` pipe ile template'de subscription yönetimi.

**UX/UI Standartları:**
- Her async işlemde loading state.
- Her başarı/hata durumunda toast bildirimi.
- Her destructive aksiyon için onay dialog'u.
- Boş state'ler için görsel ve aksiyon butonu.
- Form validasyonları reactive form ile, hata mesajları anlık.
- Tüm tıklanabilir elementler `:hover`, `:focus`, `:active` state'lerine sahip.
- `cursor: pointer` tüm buton ve linklerde.
- Erişilebilirlik: `aria-label`, `alt` metin, `tabindex` gerektiği yerde.

**Çıktı Formatı:**
- Değişen her dosyayı tam olarak ver: dosya yolu + tam içerik.
- Değişmeyen dosyaları tekrar yazma.
- Her görevin başında hangi dosyaların değiştiğini listele.
- Eğer backend değişikliği gerekiyorsa Spring Boot dosyasını da ver.
- Herhangi bir görevi başlamadan önce o göreve ait dosyaları oku/analiz et, sonra yaz.

**Hata Önleme:**
- Her HTTP çağrısı `catchError` ile sarılmalı.
- Her subscribe `takeUntilDestroyed()` veya `async` pipe ile yönetilmeli.
- `null`/`undefined` guard'lar konulmalı.
- Skeleton ve empty state hiçbir görevde eksik bırakılmamalı.