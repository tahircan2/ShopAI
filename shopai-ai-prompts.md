# ShopAI — AI Geliştirici Prompt Kılavuzu
## Chatbot + Dashboard Özellikleri için Hazır Promptlar

> Bu dosyadaki her prompt, doğrudan Claude / GPT-4o / Gemini'ye yapıştırılabilir.
> Projenin mevcut mimarisine (Angular 19 Standalone · Spring Boot 3 · MySQL 8 · LangGraph Python · HttpOnly Cookie JWT) göre yazılmıştır.

---

## BÖLÜM 1 — LANGGRAPH AI CHATBOT: TAM MİMARİ

### PROMPT 1.1 — LangGraph Multi-Agent Text2SQL Chatbot (Ana Mimari)

```
Sen bir senior Python + LangGraph geliştiricisisin.

PROJE: ShopAI e-ticaret platformu için Multi-Agent Text2SQL Chatbot geliştireceğiz.
STACK: Python 3.11+, FastAPI, LangGraph 0.2+, LangChain 0.3+, OpenAI GPT-4o-mini, MySQL 8, Pandas

MİMARİ GENEL BAKIŞ:
Kullanıcı doğal dilde soru sorar → Guardrails Agent kapsam kontrolü yapar → SQL Agent sorguya çevirir
→ MySQL'de güvenle çalıştırılır → Analysis Agent sonucu açıklar → Visualization Agent grafik üretir.

VERİTABANI ŞEMASI (MySQL 8, shopai DB):
- users (id, email, password_hash, role ENUM('USER','ADMIN','CORPORATE'), first_name, last_name, created_at)
- stores (id, owner_id FK→users.id, name, status ENUM('OPEN','CLOSED'), created_at)
- customer_profiles (id, user_id FK→users.id, age, city, membership_type ENUM('Bronze','Silver','Gold'), total_spend DECIMAL(12,2))
- categories (id, name, parent_id FK→categories.id)
- products (id, store_id FK→stores.id, category_id FK→categories.id, sku VARCHAR(100), name VARCHAR(255), unit_price DECIMAL(10,2), stock_quantity INT, rating_avg DECIMAL(3,2), is_active BOOLEAN, created_at)
- orders (id, user_id FK→users.id, store_id FK→stores.id, status ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED'), grand_total DECIMAL(10,2), payment_method VARCHAR(50), created_at)
- order_items (id, order_id FK→orders.id, product_id FK→products.id, quantity INT, unit_price DECIMAL(10,2))
- shipments (id, order_id FK→orders.id, warehouse VARCHAR(100), mode_of_shipment VARCHAR(50), status ENUM('PENDING','IN_TRANSIT','DELIVERED','RETURNED'), tracking_number VARCHAR(100), created_at)
- reviews (id, user_id FK→users.id, product_id FK→products.id, star_rating TINYINT CHECK(1-5), comment TEXT, sentiment VARCHAR(20), helpful_votes INT, created_at)

GÜVENLIK KURALLARI (DEĞİŞTİRİLEMEZ):
1. user_id HİÇBİR ZAMAN kullanıcı mesajından alınmaz. Her zaman Spring Boot'un ilettiği
   X-Authenticated-User-Id header'ından extract edilir.
2. role HİÇBİR ZAMAN kullanıcı mesajından alınmaz. Her zaman X-Authenticated-User-Role
   header'ından alınır.
3. Kullanıcı girdisi SystemMessage'a concat edilmez — her zaman ayrı HumanMessage nesnesi.
4. SQL injection'a karşı: sadece SELECT sorguları; UPDATE/DELETE/DROP/INSERT yasak.
5. Role scope'u zorunlu:
   - role=USER → sadece kendi user_id'siyle ilgili veriler (orders, reviews, shipments)
   - role=CORPORATE → sadece kendi store_id'sine ait veriler (products, orders, customers)
   - role=ADMIN → tüm platforma erişim

AGENT TANIMLARI:
1. GuardrailsAgent — Soruyu sınıflandır: GREETING / OUT_OF_SCOPE / IN_SCOPE
   - GREETING ise: sıcak karşılama mesajı döndür, bitir
   - OUT_OF_SCOPE ise: "Üzgünüm, sadece e-ticaret verileri hakkında sorulara yanıt verebilirim." döndür, bitir
   - IN_SCOPE ise: SQL aşamasına geç

2. SQLAgent — Doğal dili geçerli MySQL sorgusuna çevir
   - Sadece SQL döndür, açıklama yok, markdown yok
   - Schema'daki tablo ve kolon isimlerini kullan
   - Her zaman role scope koşulunu WHERE'e ekle
   - LIMIT 1000 ekle

3. ErrorAgent — SQL hatalıysa tanıla ve düzelt (max 3 deneme)

4. AnalysisAgent — Query sonucunu doğal dilde açıkla, key insight'ları çıkar

5. DecideVisualizationAgent — Grafik gerekli mi?
   - Gerekli: zaman serisi, karşılaştırma, dağılım, sıralama soruları
   - Gereksiz: tek sayı, basit liste

6. VisualizationAgent — Plotly chart kod üret (executable Python, markdown yok)

AgentState TypedDict:
{
  question: str,
  user_id: str,
  user_role: str,   # USER | CORPORATE | ADMIN
  store_id: str | None,
  sql_query: str | None,
  query_result: list[dict] | None,
  error: str | None,
  final_answer: str | None,
  visualization_code: str | None,
  visualization_type: str | None,   # bar | line | pie | table | None
  is_in_scope: bool,
  is_greeting: bool,
  iteration_count: int
}

LANGGRAPH GRAPH AKIŞI:
START → guardrails_node
guardrails_node → [greeting_end | out_of_scope_end | sql_node]  (conditional)
sql_node → execute_sql_node
execute_sql_node → [error_node | analysis_node]  (conditional: error varsa error_node)
error_node → execute_sql_node  (max iteration_count=3, sonra fail_end)
analysis_node → decide_viz_node
decide_viz_node → [viz_node | END]  (conditional)
viz_node → END

ÇIKTI FORMATI (JSON):
{
  "answer": "Doğal dil açıklaması...",
  "sql_query": "SELECT ...",
  "visualization_needed": true/false,
  "visualization_type": "bar|line|pie|scatter|table|null",
  "plotly_code": "import plotly.graph_objects as go\n...",
  "data_summary": { "row_count": N, "columns": [...] },
  "agent_path": ["guardrails", "sql", "execute", "analysis", "viz"]
}

UYGULAMA:
Yukarıdaki tüm detaylara göre eksiksiz çalışan kodu yaz:
- ai-service/agents/state.py  (TypedDict)
- ai-service/agents/guardrails_agent.py
- ai-service/agents/sql_agent.py
- ai-service/agents/error_agent.py
- ai-service/agents/analysis_agent.py
- ai-service/agents/decide_viz_agent.py
- ai-service/agents/viz_agent.py
- ai-service/graph/agent_graph.py  (LangGraph StateGraph)
- ai-service/main.py  (FastAPI /chat endpoint, X-Authenticated-User-Id header zorunlu)

Her dosyayı tam olarak ver. Type annotation ve docstring ekle.
```

---

### PROMPT 1.2 — Chatbot'un Türkçe/İngilizce Karma Soru Desteği

```
Mevcut ShopAI LangGraph chatbot'una Türkçe + İngilizce dil desteği ekle.

KURAL: Kullanıcı Türkçe soruyorsa Türkçe yanıt ver, İngilizce soruyorsa İngilizce yanıt ver.

TÜRKÇE → İNGİLİZCE SORGU EŞLEŞTİRMELERİ (SQL Agent system prompt'una ekle):
"puan dağılımını göster" / "show rating distribution" → star_rating bazlı GROUP BY
"geçen ay satışlar" / "last month sales" → created_at BETWEEN ... AND ...
"en çok satan ürünler" / "top selling products" → SUM(quantity) DESC
"sipariş durumu dağılımı" → status GROUP BY COUNT
"aylık gelir trendi" / "monthly revenue trend" → MONTH(created_at), SUM(grand_total)
"müşteri segmentleri" → membership_type GROUP BY
"kategoriye göre satış" / "sales by category" → JOIN categories
"en yüksek puanlı ürünler" / "highest rated products" → rating_avg DESC
"iptal oranı" / "cancellation rate" → status='CANCELLED' / total
"haftalık gelir" / "weekly revenue" → WEEK(created_at)
"sevkiyat modu dağılımı" / "shipment mode distribution" → mode_of_shipment GROUP BY
"bekleyen siparişler" / "pending orders" → status='PENDING'

GuardrailsAgent'ın OUT_OF_SCOPE tespiti için Türkçe örnekler de ekle:
OUT_OF_SCOPE: "hava durumu", "spor sonuçları", "kod yaz", "şaka anlat", "resim çiz"
GREETING: "merhaba", "selam", "nasılsın", "iyi günler", "hi", "hello"

GuardrailsAgent system prompt'unu güncelle ve Türkçe/İngilizce her ikisi için de doğru
sınıflandırma yaptığından emin ol.

Sadece değişen dosyaları ver: guardrails_agent.py, sql_agent.py
```

---

### PROMPT 1.3 — Visualization Agent: Plotly Chart Tipleri

```
ShopAI LangGraph chatbot'undaki VisualizationAgent'ı geliştir.

Aşağıdaki 6 chart tipi için özel Plotly kod şablonları yaz.
Her şablon; koyu tema (#0f0f13 bg, #6366f1 primary, #22d3ee accent),
başlık, eksen etiketleri ve responsive layout içermeli.

1. BAR CHART (Karşılaştırma soruları için)
   Örnek: "puan dağılımını göster", "kategoriye göre satış"
   - Dikey bar, yuvarlak köşe efekti
   - Her bar üzerinde değer etiketi

2. LINE CHART (Trend soruları için)
   Örnek: "aylık gelir trendi", "haftalık satışlar"
   - Smooth curve (spline), dolgu gradient
   - Hover tooltip: tarih + değer + önceki döneme göre % değişim

3. PIE / DONUT CHART (Dağılım soruları için)
   Örnek: "sipariş durumu dağılımı", "sevkiyat modu dağılımı"
   - Donut (hole=0.4), legend sağda
   - Slice'lara hover animasyonu

4. HORIZONTAL BAR (Sıralama soruları için)
   Örnek: "en çok satan ürünler top 10", "en yüksek puanlı ürünler"
   - Yatay, sıralı (büyükten küçüğe)
   - Değer bar'ın sağında gösterilsin

5. SCATTER / BUBBLE (Korelasyon soruları için)
   Örnek: "fiyat ve puan ilişkisi"
   - Bubble size = quantity veya sales
   - Korelasyon çizgisi (trendline)

6. DATA TABLE (Detaylı liste soruları için)
   Örnek: "bekleyen siparişleri listele", "en son 10 sipariş"
   - Plotly Table, koyu tema
   - İlk kolon bold, alternating row colors (#1a1a2e / #16213e)

VisualizationAgent, query_result (list[dict]) ve visualization_type alarak
doğru şablona yönlendirecek şekilde güncelle.

Çıktı: visualization_code (string, executable Python Plotly kodu),
visualization_type (bar|line|pie|horizontal_bar|scatter|table)

Sadece viz_agent.py dosyasını ver.
```

---

## BÖLÜM 2 — SPRING BOOT: CHATBOT PROXY + SQL EXECUTION

### PROMPT 2.1 — AiController ve SQL Execution Servisi

```
Spring Boot 3, Java 21. ShopAI projesinde AI chatbot için backend katmanını yaz.

GENEL AKIŞ:
Angular → POST /api/ai/chat → Spring Boot AiController → Python FastAPI /chat (proxy)
Python yanıtı geri gelince → Angular'a ilet

SINIFLAR:

1. AiController.java (/api/ai/chat endpoint'i):
   - @PostMapping("/api/ai/chat")
   - @PreAuthorize("isAuthenticated() or permitAll()")
   - Request body: { sessionId: String, message: String }
   - JWT cookie'den userId ve role extract et (SecurityContextHolder)
   - Python servisine şu header'larla ilet:
     X-Authenticated-User-Id: {userId}
     X-Authenticated-User-Role: {role}   (USER | CORPORATE | ADMIN)
     X-Store-Id: {storeId if CORPORATE}
   - Python yanıtını doğrudan frontend'e dön
   - Konuşmayı ai_conversations + ai_messages tablolarına kaydet
   - @RateLimiter(name="chatbot", fallback = "rateLimitFallback") ekle

2. AiService.java:
   - WebClient ile Python servisine async HTTP çağrısı
   - Timeout: 30 saniye
   - Retry: 2 kez (connection error durumunda)
   - Python servis URL'i: application.yml'den oku (app.ai-service.url)

3. SqlExecutionService.java (SADECE SELECT izni olan güvenli executor):
   - Bu servis isteğe bağlı — Python servis doğrudan DB'ye de bağlanabilir
   - Eğer Spring Boot üzerinden çalıştırılacaksa:
     * Gelen SQL'i validate et: sadece SELECT ile başlamalı
     * Tehlikeli keyword kontrolü: DROP, DELETE, UPDATE, INSERT, EXEC, --
     * EntityManager.createNativeQuery ile çalıştır
     * Sonucu List<Map<String, Object>> olarak döndür
     * Max 1000 satır limit

4. AiChatRequest.java (DTO):
   { sessionId: String, message: String }

5. AiChatResponse.java (DTO):
   { answer: String, sqlQuery: String, visualizationNeeded: Boolean,
     visualizationType: String, plotlyCode: String, agentPath: List<String> }

application.yml'e ekle:
app:
  ai-service:
    url: http://localhost:8000
    timeout: 30000

Sadece bu dosyaları ver. userId her zaman SecurityContextHolder'dan alınmalı,
request body'den userId kabul edilmemeli.
```

---

## BÖLÜM 3 — ANGULAR: CHATBOT COMPONENT

### PROMPT 3.1 — ChatbotComponent (Tam Özellikli)

```
Angular 19 standalone component, inject() pattern, OnPush CD.

BILEŞEN: ChatbotComponent — ShopAI projesinin tam özellikli AI chatbot ekranı.

TASARIM (PDF'deki DataPulse AI ekranına benzer, koyu tema):
- Background: #0a0a0f
- Primary: #6366f1 (indigo)
- Accent: #22d3ee (cyan)
- Surface: #1a1a2e
- Border: rgba(99,102,241,0.2)
- Font: 'JetBrains Mono' (code), 'Inter' (text) — Google Fonts'tan yükle

LAYOUT:
- Tam ekran chat arayüzü (pages/ai-chat.component)
- Sol sidebar: Konuşma geçmişi listesi (session'lar)
- Sağ alan: Aktif konuşma + mesaj girişi
- Header: "AI Data Assistant" başlığı + yeşil "Online" badge + kullanıcı rolü badge'i

MESAJ GÖRÜNÜMÜ:
- Kullanıcı mesajları: sağ tarafta, indigo bg, kullanıcı avatarı
- AI yanıtları: sol tarafta, surface bg, bot avatarı (⊕ ikonu)
- Her AI yanıtı şunları içerebilir:
  a) Metin açıklaması
  b) SQL sorgusu (collapsible code block, syntax highlight, kopyala butonu)
  c) Grafik (PlotlyChartComponent — ayrı component, aşağıya bak)
  d) Tablo (DataTableComponent — ayrı component)
  e) Özet kartlar (ör. "3 key insights")
- Mesaj altında: agent path göstergesi (küçük chip'ler: guardrails → sql → analysis → viz)
- Yazıyor animasyonu: 3 noktalı pulse (AI yanıt beklenirken)

GİRİŞ ALANI:
- Alt kısımda sabit, tam genişlik
- Placeholder: "Ask me to analyze and visualize your data..."
- Gönder butonu (Enter veya tıklama)
- Karakter sayacı (max 500)
- Örnek soru chip'leri (role'e göre değişir):
  * USER: "Sipariş geçmişimi göster", "En çok harcadığım kategoriler"
  * CORPORATE: "Bu haftaki satışlar", "En çok satan ürünler top 10"
  * ADMIN: "Platform geneli gelir trendi", "Tüm mağazaların karşılaştırması"

CHAT SERVİSİ (ai-chat.service.ts):
- sendMessage(sessionId, message): Observable<AiChatResponse>
- POST /api/ai/chat, withCredentials: true
- getConversationHistory(sessionId): Observable<ChatMessage[]>
- sessions$: BehaviorSubject<ChatSession[]>
- currentSession$: BehaviorSubject<ChatSession | null>
- createNewSession(): void — yeni UUID oluştur

MODELLER (chat.model.ts):
interface AiChatResponse {
  answer: string;
  sqlQuery?: string;
  visualizationNeeded: boolean;
  visualizationType?: 'bar'|'line'|'pie'|'horizontal_bar'|'scatter'|'table';
  plotlyCode?: string;
  dataSummary?: { rowCount: number; columns: string[] };
  agentPath: string[];
}
interface ChatMessage {
  id: string;
  role: 'user'|'assistant';
  content: string;
  response?: AiChatResponse;
  timestamp: Date;
}
interface ChatSession {
  id: string;
  title: string;
  lastMessage: string;
  createdAt: Date;
}

DOSYALAR:
- src/app/features/ai-chat/ai-chat.component.ts
- src/app/features/ai-chat/ai-chat.component.html
- src/app/features/ai-chat/ai-chat.component.scss
- src/app/core/services/ai-chat.service.ts
- src/app/core/models/chat.model.ts

Angular 19 standalone, inject() ile AuthService ve AiChatService kullan.
Router route: { path: 'ai-chat', component: AiChatComponent, canActivate: [authGuard] }
Sadece bu dosyaları ver.
```

---

### PROMPT 3.2 — PlotlyChartComponent (Grafik Render)

```
Angular 19 standalone component. ShopAI projesi için Plotly grafik render bileşeni.

AMAÇ: Python VisualizationAgent'ın ürettiği Plotly JSON spec'ini Angular'da render et.

BILEŞEN: PlotlyChartComponent

YAKLAŞIM: Python tarafında Plotly figürü JSON olarak serialize edilir (fig.to_json()),
Angular bu JSON'u alıp Plotly.js ile render eder.

KURULUM: npm install plotly.js-dist-min @types/plotly.js

BILEŞEN DETAYI:
@Input() plotlyJson: string  — Python'dan gelen fig.to_json() çıktısı
@Input() height: number = 350
@Input() title: string = ''

- plotlyJson değişince chart'ı yeniden render et (ngOnChanges)
- Plotly.react() kullan (ilk render: Plotly.newPlot)
- Resize observer: container boyutu değişince Plotly.relayout
- Hata durumunda: "Grafik yüklenemedi" mesajı + retry butonu
- Loading durumunda: pulse shimmer animasyonu

Python tarafında VisualizationAgent çıktısını şu şekilde güncelle:
fig = go.Figure(...)
response["plotly_json"] = fig.to_json()  # JSON string olarak

AiChatResponse modeline plotlyJson: string alanı ekle.

DOSYALAR:
- src/app/shared/components/plotly-chart/plotly-chart.component.ts
- src/app/shared/components/plotly-chart/plotly-chart.component.html
- src/app/shared/components/plotly-chart/plotly-chart.component.scss

Sadece bu dosyaları ver.
```

---

### PROMPT 3.3 — DataTableComponent (Tablo Render)

```
Angular 19 standalone component. ShopAI chatbot için dinamik data table bileşeni.

BILEŞEN: ChatDataTableComponent
Chatbot yanıtında "table" tipinde visualization geldiğinde kullanılır.

@Input() data: Record<string, unknown>[]  — query_result dizisi
@Input() maxRows: number = 50

ÖZELLİKLER:
- Kolonlar data'dan otomatik çıkarılır (Object.keys)
- Sayfalama: max 10 satır/sayfa, sayfa numaraları
- Kolon başlığına tıklayınca sıralama (asc/desc)
- Arama/filtre input'u (kolonları filtrele)
- CSV olarak indir butonu
- Sayısal değerleri sağa yasla, string değerleri sola
- NULL/undefined değerleri "—" göster
- Uzun string'ler truncate (max 50 karakter, hover'da tam metin tooltip)

TASARIM (koyu tema):
- Header: #1a1a2e bg, #6366f1 text, sort ikonu
- Satırlar: alternating #0f0f13 / #1a1a2e
- Hover: rgba(99,102,241,0.1) bg
- Border: rgba(99,102,241,0.15)

DOSYALAR:
- src/app/shared/components/chat-data-table/chat-data-table.component.ts
- src/app/shared/components/chat-data-table/chat-data-table.component.html
- src/app/shared/components/chat-data-table/chat-data-table.component.scss

Sadece bu dosyaları ver.
```

---

## BÖLÜM 4 — DASHBOARD PANELLERİ

### PROMPT 4.1 — Corporate Dashboard (Mağaza Sahibi Paneli)

```
Angular 19 standalone component, inject() pattern, OnPush CD.
ShopAI projesi. Corporate User Dashboard — mağaza sahibi ana paneli.

TASARIM: DataPulse örneğindeki gibi koyu analytics dashboard.
- Background: #0a0a0f
- Primary accent: #6366f1
- Success: #10b981, Warning: #f59e0b, Danger: #ef4444, Info: #22d3ee
- Card surface: #1a1a2e
- Font: Outfit (Google Fonts) — display, Inter — body

LAYOUT (CSS Grid):
- Üst: 4 KPI kartı yan yana (responsive: 2 kolon mobil, 4 kolon desktop)
- Orta sol: Revenue trend line chart (Chart.js, son 7 gün)
- Orta sağ: Sales by category pie/donut chart
- Alt sol: Son siparişler tablosu (son 10)
- Alt sağ: Top 5 ürün horizontal bar chart

KPI KARTLARI:
1. Total Revenue — GET /api/corporate/stats/revenue — ikon: 💰
2. Total Orders — GET /api/corporate/stats/orders — ikon: 📦
3. Active Customers — GET /api/corporate/stats/customers — ikon: 👥
4. Avg Rating — GET /api/corporate/stats/avg-rating — ikon: ⭐
Her kart: büyük sayı + % değişim badge (yeşil/kırmızı ok)

SON SİPARİŞLER TABLOSU:
Kolonlar: Order ID | Customer | Products | Total | Date | Status
Status badge: PENDING=sarı, CONFIRMED=mavi, SHIPPED=mor, DELIVERED=yeşil, CANCELLED=kırmızı

SERVİS (corporate-stats.service.ts):
- getKpiStats(): Observable<KpiStats>
- getRevenueChart(days: 7|30|90): Observable<RevenueChartData[]>
- getCategoryChart(): Observable<CategoryChartData[]>
- getRecentOrders(): Observable<OrderSummary[]>
- getTopProducts(): Observable<TopProduct[]>

Tüm istekler: GET /api/corporate/stats/*, withCredentials: true

Chart.js kurulumu: import { Chart, registerables } from 'chart.js'; Chart.register(...registerables);
NgChartsModule kullan: npm install ng2-charts chart.js

DOSYALAR:
- src/app/features/admin/corporate-dashboard/corporate-dashboard.component.ts
- src/app/features/admin/corporate-dashboard/corporate-dashboard.component.html
- src/app/features/admin/corporate-dashboard/corporate-dashboard.component.scss
- src/app/core/services/corporate-stats.service.ts

Sadece bu dosyaları ver.
```

---

### PROMPT 4.2 — Admin Dashboard (Platform Yöneticisi Paneli)

```
Angular 19 standalone component, inject() pattern, OnPush CD.
ShopAI projesi. Admin Dashboard — platform geneli yönetici paneli.

TASARIM: Corporate Dashboard ile aynı design system, biraz daha kapsamlı.

LAYOUT (CSS Grid, 12 kolon):
- Üst: 4 Platform KPI kartı (total users, total stores, platform revenue, avg order value)
- 2. Satır: Platform revenue line chart (tüm mağazalar, son 30 gün)
- 3. Satır sol: Top 5 store comparison horizontal bar
- 3. Satır sağ: User role dağılımı pie chart (USER/CORPORATE/ADMIN oranları)
- 4. Satır: En son audit log tablosu (son 20 kritik eylem)
- 5. Satır: Mağaza listesi tablosu (store adı, owner, status, revenue, order count)

KPI KARTLARI:
1. Total Platform Revenue — GET /api/admin/stats/platform-revenue
2. Total Users — GET /api/admin/stats/users
3. Active Stores — GET /api/admin/stats/stores
4. Avg Order Value — GET /api/admin/stats/aov

MAĞAZA YÖNETİM TABLOSU:
Kolonlar: Store Name | Owner | Status | Revenue | Orders | Action
Action: "Open/Close" toggle butonu → PUT /api/admin/stores/{id}/status

AUDIT LOG TABLOSU:
Kolonlar: Time | User | Action | Entity | IP
Kritik aksiyonlar renkli: LOGIN_FAILED=kırmızı, ORDER_CREATED=yeşil, INJECTION_DETECTED=kırmızı bold

SERVİS (admin-stats.service.ts):
- getPlatformStats(): Observable<PlatformStats>
- getPlatformRevenue(days: number): Observable<RevenuePoint[]>
- getStoreComparison(): Observable<StoreStats[]>
- getRecentAuditLogs(): Observable<AuditLog[]>
- toggleStoreStatus(storeId: number, status: 'OPEN'|'CLOSED'): Observable<void>

DOSYALAR:
- src/app/features/admin/admin-dashboard/admin-dashboard.component.ts
- src/app/features/admin/admin-dashboard/admin-dashboard.component.html
- src/app/features/admin/admin-dashboard/admin-dashboard.component.scss
- src/app/core/services/admin-stats.service.ts

Sadece bu dosyaları ver.
```

---

### PROMPT 4.3 — Individual User Dashboard (Müşteri Paneli)

```
Angular 19 standalone component, inject() pattern, OnPush CD.
ShopAI projesi. Individual User — kişisel harcama analitik paneli.

TASARIM: Diğer dashboardlarla aynı design system ama daha "kişisel" hissettiren.

LAYOUT:
- Üst: 3 Kişisel KPI (Toplam Harcama, Sipariş Sayısı, Ortalama Sipariş)
- Orta: Aylık harcama line chart (son 6 ay)
- Sağ: Kategoriye göre harcama donut chart
- Alt sol: Son siparişler tablosu
- Alt sağ: Membership durumu kartı (Bronze/Silver/Gold badge + ilerleme)

KPI KARTLARI:
1. Total Spend — bu kullanıcının toplam harcaması
2. Total Orders — bu kullanıcının sipariş sayısı
3. Avg Order Value — toplam / sipariş sayısı

MEMBERSHIP KARTI:
- Bronze: 0-500₺, Silver: 500-2000₺, Gold: 2000₺+
- İlerleme çubuğu: bir sonraki seviyeye kalan miktar
- Seviye ikonu (emoji veya SVG)

SIPARIŞ TABLOSU:
Kolonlar: Order # | Date | Items | Total | Status | Detail →
Status badge: renkli
Detail → butonu: sipariş detay sayfasına git

Tüm veriler JWT'den alınan userId ile scope edilir. Controller'a userId parametre verilmez.

SERVİS (user-stats.service.ts):
- getPersonalStats(): Observable<PersonalStats>
- getMonthlySpend(): Observable<MonthlySpend[]>
- getCategorySpend(): Observable<CategorySpend[]>
- getRecentOrders(): Observable<OrderSummary[]>

DOSYALAR:
- src/app/features/profile/user-dashboard/user-dashboard.component.ts
- src/app/features/profile/user-dashboard/user-dashboard.component.html
- src/app/features/profile/user-dashboard/user-dashboard.component.scss
- src/app/core/services/user-stats.service.ts

Sadece bu dosyaları ver.
```

---

## BÖLÜM 5 — SPRING BOOT: STATS ENDPOINT'LERİ

### PROMPT 5.1 — Corporate Stats Controller

```
Spring Boot 3, Java 21, JPA. ShopAI projesi.
Corporate User için analytics istatistik endpoint'leri yaz.

SINIF: CorporateStatsController (/api/corporate/stats)
Tüm endpoint'ler @PreAuthorize("hasRole('CORPORATE')") ile korunur.
storeId HER ZAMAN JWT'den extract edilen userId üzerinden bulunur:
  Store store = storeRepository.findByOwnerId(userId).orElseThrow(...)

ENDPOINT'LER:

GET /api/corporate/stats/revenue
→ Kullanıcının store'una ait orders tablosundan son 7, 30, 90 güne göre filtreli
  Response: { totalRevenue: BigDecimal, change: Double (% değişim önceki döneme göre) }

GET /api/corporate/stats/orders
→ store'a ait order sayısı + % değişim
  Response: { totalOrders: Long, change: Double }

GET /api/corporate/stats/customers
→ store'a sipariş veren distinct user sayısı
  Response: { totalCustomers: Long, newThisMonth: Long }

GET /api/corporate/stats/avg-rating
→ store'daki ürünlerin ortalama rating_avg
  Response: { avgRating: Double, change: Double }

GET /api/corporate/stats/revenue-chart?days=7 (default 7, diğer: 30, 90)
→ Günlük bazda gelir
  Response: [ { date: "2025-04-18", revenue: 1250.00 }, ... ]

GET /api/corporate/stats/category-chart
→ Kategoriye göre toplam satış tutarı
  Response: [ { category: "Electronics", revenue: 5200.00, percentage: 42.0 }, ... ]

GET /api/corporate/stats/recent-orders?limit=10
→ En son siparişler (order number, customer name, total, status, created_at)
  Response: [ { orderNumber, customerName, itemCount, total, status, createdAt }, ... ]

GET /api/corporate/stats/top-products?limit=5
→ En çok satan ürünler (quantity toplamına göre)
  Response: [ { productName, totalQuantity, totalRevenue, rating }, ... ]

Tüm sorgular @Query ile JPQL olarak yaz.
Sadece değişen dosyaları ver: CorporateStatsController.java, CorporateStatsService.java,
CorporateStatsResponse.java (inner record'lar olarak)
```

---

### PROMPT 5.2 — Admin Stats Controller

```
Spring Boot 3, Java 21, JPA. ShopAI projesi.
Admin için platform geneli istatistik endpoint'leri yaz.

SINIF: AdminStatsController (/api/admin/stats)
Tüm endpoint'ler @PreAuthorize("hasRole('ADMIN')") ile korunur.

ENDPOINT'LER:

GET /api/admin/stats/platform-revenue
→ Tüm mağazaların toplam geliri + önceki aya göre % değişim

GET /api/admin/stats/users
→ Toplam kullanıcı sayısı + bu ay yeni kayıtlar + role dağılımı
  Response: { total: Long, newThisMonth: Long, byRole: { USER: N, CORPORATE: N, ADMIN: N } }

GET /api/admin/stats/stores
→ Toplam mağaza + aktif (OPEN) sayısı + bu ay açılan
  Response: { total: Long, active: Long, newThisMonth: Long }

GET /api/admin/stats/aov
→ Platform geneli ortalama sipariş değeri

GET /api/admin/stats/platform-revenue-chart?days=30
→ Günlük platform geneli gelir

GET /api/admin/stats/store-comparison
→ Top 10 mağaza, gelire göre sıralı
  Response: [ { storeName, ownerName, revenue, orderCount, status }, ... ]

GET /api/admin/stats/user-role-distribution
→ Role bazlı kullanıcı dağılımı (pie chart için)

GET /api/admin/stats/audit-logs?limit=20
→ En son audit log kayıtları (kritik eylemler)
  Response: [ { timestamp, userEmail, action, entityType, ipAddress }, ... ]

PUT /api/admin/stores/{id}/status
→ Mağaza aç/kapat
  Body: { status: "OPEN" | "CLOSED" }

Sadece değişen dosyaları ver: AdminStatsController.java, AdminStatsService.java
```

---

## BÖLÜM 6 — NAVIGATION & ROUTING

### PROMPT 6.1 — Sidebar Navigation (Role'e Göre Dinamik)

```
Angular 19 standalone component, inject() pattern.
ShopAI projesi. Sol sidebar navigasyon bileşeni — role'e göre dinamik menü.

BILEŞEN: SidebarComponent (shared)

TASARIM (DataPulse benzeri):
- Sabit genişlik: 240px, koyu bg (#0f0f13), sağ kenarda ince border
- Logo üstte (ShopAI + ikon)
- "MAIN MENU" ve "MANAGEMENT" bölümleri (section header)
- Aktif route highlight: indigo bg, sol kenar çizgisi
- Hover efekti: hafif indigo tint
- Alt kısım: kullanıcı avatar + isim + rol badge + logout butonu

ROLE'E GÖRE MENÜ ÖĞELERİ:

role = USER:
  MAIN MENU: Dashboard 🏠, Orders 📦, Products 🛍️, Wishlist ❤️, AI Assistant ⊕ [NEW]
  ACCOUNT: Profile 👤, Notifications 🔔

role = CORPORATE:
  MAIN MENU: Dashboard 🏠, AI Assistant ⊕ [NEW], Analytics 📊, Orders 📦, Products 🏷️
  MANAGEMENT: Customers 👥, Store Settings ⚙️, Shipments 🚚, Reviews ⭐

role = ADMIN:
  MAIN MENU: Dashboard 🏠, AI Assistant ⊕ [NEW], Analytics 📊, All Orders 📦
  MANAGEMENT: Users 👤, Stores 🏪, Categories 📁, Audit Logs 🔍

NEW badge: AI Assistant menü öğesinde animasyonlu kırmızı/mor badge

AuthService'ten currentUser$ ile rol bilgisini al.
Router.url ile aktif route'u tespit et.

DOSYALAR:
- src/app/shared/components/sidebar/sidebar.component.ts
- src/app/shared/components/sidebar/sidebar.component.html
- src/app/shared/components/sidebar/sidebar.component.scss

Sadece bu dosyaları ver.
```

---

## BÖLÜM 7 — ÖRNEK SORU AKIŞLARI (TEST REHBERİ)

### Chatbot'u Test Etmek için Örnek Sorgular

**CORPORATE User olarak giriş yapıp şunları dene:**
```
"puan dağılımını göster"
→ Beklenen: star_rating 1-5 için bar chart + "Müşterilerinizin %X'i 5 yıldız vermiş" analizi

"geçen haftaki günlük gelir trendini göster"
→ Beklenen: 7 günlük line chart + peak gün analizi

"en çok satan 5 ürünüm"
→ Beklenen: horizontal bar chart, ürün adı + satış adedi

"sipariş durumu dağılımı"
→ Beklenen: donut chart (DELIVERED/SHIPPED/PENDING/CANCELLED)

"kategoriye göre satış bu ay"
→ Beklenen: bar veya pie chart + kategori bazlı özet

"bekleyen siparişleri listele"
→ Beklenen: data table (order_id, customer, date, total, status)

"compare this month vs last month"
→ Beklenen: iki dönem karşılaştırma bar chart

"sevkiyat modu dağılımını göster"
→ Beklenen: pie chart (Air/Road/Ship oranları)
```

**OUT OF SCOPE test:**
```
"hava durumu nasıl"
→ Beklenen: "Üzgünüm, sadece e-ticaret verileri hakkında sorulara yanıt verebilirim."

"bana şaka anlat"
→ Beklenen: kapsam dışı mesajı

"merhaba"
→ Beklenen: sıcak karşılama, örnek sorular
```

---

## BÖLÜM 8 — BÜTÜNLEŞIK KURULUM KONTROLÜ

### PROMPT 8.1 — End-to-End Entegrasyon Testi

```
ShopAI projesinin chatbot ve dashboard entegrasyonunu doğrula.

Aşağıdaki adımları sırayla uygula ve her adımda neyin başarılı/başarısız
olduğunu logla:

ADIM 1 — Python AI Servisi:
1. uvicorn main:app --reload ile başlat
2. GET http://localhost:8000/health → {"status": "ok"} beklenir
3. Curl ile test (CORPORATE role):
   curl -X POST http://localhost:8000/chat \
     -H "Content-Type: application/json" \
     -H "X-Authenticated-User-Id: 1" \
     -H "X-Authenticated-User-Role: CORPORATE" \
     -H "X-Store-Id: 1" \
     -d '{"session_id": "test-123", "message": "puan dağılımını göster"}'
   Beklenen: answer + plotly_json

ADIM 2 — Spring Boot Backend:
1. mvn spring-boot:run ile başlat
2. Login ol: POST /api/auth/login → cookie set edilmeli
3. Chat proxy test:
   POST /api/ai/chat { "sessionId": "test-123", "message": "puan dağılımını göster" }
   Cookie header ile → Python'a proxy → yanıt dönmeli

ADIM 3 — Angular Frontend:
1. ng serve ile başlat
2. Login → role bazlı dashboard açılmalı
3. AI Chat sayfasına git → örnek sorulardan birine tıkla
4. Yanıt: metin + grafik render edilmeli

HATA AYIKLAMA KILAVUZU:
- Python 500: SQL syntax hatası → error_agent devreye girmeli (max 3 retry)
- Spring 401: Cookie eksik → withCredentials: true kontrol et
- Angular grafik görünmüyor: PlotlyChartComponent'te ngOnChanges ve Plotly.react() kontrol et
- CORS hatası: Spring SecurityConfig'de allowedOrigins ve allowCredentials(true) kontrol et

Herhangi bir adımda hata alırsan hangi component'te olduğunu ve nasıl düzeltileceğini açıkla.
```

---

*ShopAI Prompt Kılavuzu — Chatbot + Dashboard Edition*
*Her prompt bağımsız olarak kullanılabilir. Sıra önerilir: Bölüm 1 → 2 → 3 → 4 → 5 → 6*
