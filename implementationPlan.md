# Agentic UI Control — Kapsamlı Entegrasyon Planı

**todo2.md planının mevcut ShopAI projesine uyumlu, eksikleri giderilmiş, production-ready entegrasyon rehberi**

---

## Mevcut Proje Analizi

Projenin mevcut durumu incelendi. Çalışan 3 servis tespit edildi:

| Katman | Durum | Mevcut Yapı |
|--------|-------|-------------|
| **Backend** (Spring Boot 3, Java 21) | ✅ Çalışıyor | 8 Controller, 17 Service, 20 Entity, 19 Repository |
| **Frontend** (Angular 19) | ✅ Çalışıyor | 14 Feature modülü, 11 Service, 9 Shared component |
| **AI Service** (Python FastAPI + LangGraph) | ✅ Çalışıyor | 6 Agent, Supervisor routing, SSE streaming |

### Mevcut Agent'lar (AI Service)
- `supervisor.py` — Intent classification (PRODUCT_FILTER, CART_ACTION, RECOMMENDATION, ORDER_QUERY, FAQ, GENERAL)
- `filter_agent.py` — Ürün filtreleme
- `cart_agent.py` — Sepet işlemleri (GET, ADD, REMOVE, CLEAR)
- `recommend_agent.py` — Ürün önerisi
- `order_agent.py` — Sipariş durumu
- `faq_agent.py` — SSS yanıtlama

### Mevcut Güvenlik Katmanı
- 3 katmanlı prompt injection koruması (Frontend regex, Spring Boot sanitize, Python LLM guard)
- JWT HttpOnly cookie mimarisi
- X-Authenticated-User-Id header ile servisler arası kimlik
- X-Internal-Key ile iç servis doğrulama

---

## todo2.md Planının Analizi ve Eksiklik Tespiti

> [!IMPORTANT]
> todo2.md planı güçlü bir Agentic UI Control vizyonu çiziyor ancak mevcut projeyle entegrasyon için **kritik eksiklikler** tespit edildi. Aşağıda her bir madde analiz edilmiş, eksikler doldurulmuş ve uyumsuzluklar düzeltilmiştir.

### todo2.md'de Tespit Edilen Eksiklikler

| # | Eksiklik | Açıklama |
|---|----------|----------|
| 1 | **Coupon endpoint'leri zaten kısmen var** | `CouponService.java` ve `CartController` üzerinde kupon uygulama mevcut. todo2 bunu sıfırdan planlıyor — mevcut yapı genişletilmeli |
| 2 | **AgentState genişletme planı yok** | todo2'de yeni agent'lar var ama mevcut `AgentState` TypedDict'ine hangi alanların ekleneceği belirtilmemiş |
| 3 | **SSE streaming uyumu düşünülmemiş** | Mevcut mimari SSE streaming kullanıyor. Çok adımlı işlemlerde her adımın streaming ile frontend'e iletilmesi planlanmamış |
| 4 | **Mevcut `handleAgentAction()` genişletme planı yok** | Frontend'deki `AiChatService.handleAgentAction()` sadece 4 action type destekliyor; yeni tipler için genişletme planlanmamış |
| 5 | **Veritabanı migration stratejisi yok** | Yeni tablolar eklenecek ama mevcut verilerle uyum, Hibernate ddl-auto stratejisi ve migration aracı belirtilmemiş |
| 6 | **Coupon → kullanıcıya ait kupon ilişkisi yok** | `coupons` tablosu `user_id` kolonu içermiyor; "kullanıcıya ait kupon" doğrulaması mevcut şemada yapılamaz |
| 7 | **Ödeme bilgileri tablosu eksik** | todo2 "kayıtlı ödeme yöntemleri" diyor ama sistemde `payment_methods` tablosu yok |
| 8 | **Frontend WebSocket/polling stratejisi yok** | Uzun süren agent işlemlerinde frontend'in durumu nasıl takip edeceği (polling vs WebSocket) belirtilmemiş |
| 9 | **Error recovery stratejisi detaysız** | Rollback mekanizması söyleniyor ama hangi adımların geri alınabilir olduğu, hangi Spring Boot API'lerin rollback desteklediği belirtilmemiş |
| 10 | **User AI tercihleri UI'ı eksik** | Tercih sayfası planlanmış ama Angular component yapısı ve route'u belirtilmemiş |

---

## Proposed Changes

### FAZ 1 — Veritabanı Katmanı (Öncelik: Kritik)

> Tüm yeni tablolar mevcut şemaya eklenir. Mevcut tablolara yeni kolonlar `ALTER TABLE` ile eklenir. Hibernate `ddl-auto=update` (dev) kullanıldığı için entity değişiklikleri otomatik yansır.

---

#### [NEW] `AgentTransaction.java` — Agent İşlem Geçmişi Entity

```
com.shopai.entity.AgentTransaction
├── id (BIGINT, PK)
├── userId (FK → users.id)
├── sessionId (VARCHAR 100) — AI oturumu
├── transactionType (ENUM: CHECKOUT, CART_MODIFY, NAVIGATE, COUPON_APPLY)
├── status (ENUM: PENDING, IN_PROGRESS, AWAITING_APPROVAL, COMPLETED, FAILED, CANCELLED, ROLLED_BACK)
├── totalSteps (INT)
├── completedSteps (INT)
├── failedStep (INT, nullable) — hata olan adım sırası
├── totalDurationMs (LONG, nullable)
├── tokensUsed (INT, nullable)
├── errorMessage (TEXT, nullable)
├── createdAt, updatedAt (DATETIME)
```

Index: `idx_agent_tx_user`, `idx_agent_tx_session`, `idx_agent_tx_status`

---

#### [NEW] `AgentTransactionStep.java` — Agent İşlem Adımları Entity

```
com.shopai.entity.AgentTransactionStep
├── id (BIGINT, PK)
├── transactionId (FK → agent_transactions.id)
├── stepOrder (INT) — 1, 2, 3...
├── stepType (ENUM: VALIDATE_CART, CHECK_STOCK, APPLY_COUPON, SELECT_ADDRESS, CREATE_ORDER, CLEAR_CART)
├── status (ENUM: PENDING, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK)
├── requestData (JSON, nullable) — API'ye gönderilen veri özeti
├── responseData (JSON, nullable) — API'den dönen yanıt özeti  
├── errorMessage (TEXT, nullable)
├── durationMs (LONG, nullable)
├── isRollbackable (BOOLEAN) — bu adım geri alınabilir mi
├── createdAt (DATETIME)
```

Index: `idx_agent_step_tx`

---

#### [NEW] `PendingApproval.java` — Bekleyen Onay Entity

```
com.shopai.entity.PendingApproval
├── id (BIGINT, PK)
├── userId (FK → users.id)
├── approvalToken (VARCHAR 255, UNIQUE) — UUID
├── transactionId (FK → agent_transactions.id, nullable)
├── planHash (VARCHAR 64) — SHA-256 hash, plan bütünlüğü kontrolü
├── planData (JSON) — İşlem planının tamamı {steps: [...], summary: "..."}
├── agentType (VARCHAR 50) — Hangi agent oluşturdu
├── status (ENUM: PENDING, APPROVED, REJECTED, EXPIRED)
├── expiresAt (DATETIME) — Onay son geçerlilik (oluşturma + 10 dk)
├── respondedAt (DATETIME, nullable)
├── createdAt (DATETIME)
```

Index: `idx_pending_approval_token`, `idx_pending_approval_user`, `idx_pending_approval_status`

---

#### [NEW] `UserAiPreference.java` — Kullanıcı AI Tercihleri Entity

```
com.shopai.entity.UserAiPreference
├── id (BIGINT, PK)
├── userId (FK → users.id, UNIQUE)
├── autoApproveEnabled (BOOLEAN, default FALSE)
├── autoApproveMaxAmount (DECIMAL 10,2, nullable) — Bu tutarın altı otomatik onay
├── autoApproveCategories (JSON, nullable) — Otomatik onay verilen işlem tipleri listesi
├── useDefaultAddress (BOOLEAN, default TRUE)
├── useDefaultPayment (BOOLEAN, default TRUE)
├── dailyTransactionLimit (INT, default 10) — Günlük maks AI işlem
├── maxOrderAmount (DECIMAL 10,2, default 5000) — AI ile oluşturulabilecek maks sipariş tutarı
├── createdAt, updatedAt (DATETIME)
```

---

#### [MODIFY] `AiConversation.java` — Mevcut entity'ye ek alanlar

```diff
+ @Column(name = "active_transaction_id")
+ private Long activeTransactionId; // Aktif agent işlem ID'si
+
+ @Column(name = "pending_approval_id")  
+ private Long pendingApprovalId; // Bekleyen onay ID'si
```

---

#### [MODIFY] `Order.java` — AI kaynaklı sipariş bayrağı

```diff
+ @Column(name = "is_ai_created", nullable = false)
+ @Builder.Default
+ private Boolean isAiCreated = false; // AI tarafından mı oluşturuldu
+
+ @Column(name = "ai_agent_type", length = 50)
+ private String aiAgentType; // checkout_orchestration, cart_agent vs.
+
+ @Column(name = "agent_transaction_id")
+ private Long agentTransactionId; // İlgili agent işlem ID'si
```

---

#### [MODIFY] `Cart.java` — AI müdahale takibi

```diff
+ @Column(name = "last_ai_interaction_at")
+ private LocalDateTime lastAiInteractionAt;
+
+ @Column(name = "last_ai_agent", length = 50)
+ private String lastAiAgent;
```

---

#### [MODIFY] `AuditLog.java` — AI kaynaklı işlem bayrağı

```diff
+ @Column(name = "is_ai_action", nullable = false)
+ @Builder.Default
+ private Boolean isAiAction = false;
+
+ @Column(name = "agent_transaction_id")
+ private Long agentTransactionId;
```

---

### FAZ 2 — Backend Yeni Endpoint'ler (Spring Boot)

#### [NEW] `AgentApprovalController.java` — Onay Yönetimi

```
POST   /api/agent/approvals                    — AI işlem planını onay bekler durumda kaydet
POST   /api/agent/approvals/{token}/approve     — Onay ver (token ile)
POST   /api/agent/approvals/{token}/reject      — Reddet
GET    /api/agent/approvals/{token}/status       — Onay hâlâ geçerli mi?
GET    /api/agent/approvals/pending              — Kullanıcının bekleyen onayları
```

> [!WARNING]
> `approvalToken` tek kullanımlık ve 10 dakika ömürlüdür. Onaylanan plan ile çalıştırılan plan SHA-256 hash ile karşılaştırılır. Farklılık tespit edilirse `403 PLAN_TAMPERED` döner.

---

#### [NEW] `AgentTransactionController.java` — İşlem Durumu

```
GET    /api/agent/transactions/{id}/status       — Adım adım durum (ilerleme çubuğu için)
GET    /api/agent/transactions                   — Kullanıcının tüm agent işlem geçmişi
```

---

#### [NEW] `QuickCheckoutController.java` — Hızlı Sipariş

```
POST   /api/agent/quick-checkout/validate        — Ön doğrulama (dry-run)
POST   /api/agent/quick-checkout/execute          — Tek API'de checkout (onay token zorunlu)
```

> [!IMPORTANT]
> `/execute` endpoint'i tek bir `@Transactional` içinde çalışır:
> 1. Onay token doğrula → 2. Plan hash doğrula → 3. Stok kontrol → 4. Kupon uygula → 5. Adres doğrula → 6. Sipariş oluştur → 7. Sepeti temizle
> Herhangi bir adım başarısızsa tüm işlem rollback olur.

---

#### [NEW] `AgentCouponController.java` — Kupon Sorgulama (Agent İçin)

```
GET    /api/agent/coupons/applicable             — Sepete uygulanabilir kuponlar
POST   /api/agent/coupons/dry-run                — Kupon uygulamayı simüle et (gerçekten uygulamaz)
```

> Mevcut `CouponService` genişletilir, yeni controller ayrı tutulur.

---

#### [NEW] `AgentInternalController.java` — Python AI ↔ Spring Boot İç İletişim

```
GET    /api/internal/agent/user/{userId}/default-address     — Varsayılan adres
GET    /api/internal/agent/user/{userId}/payment-methods     — Maskelenmiş ödeme yöntemleri
GET    /api/internal/agent/user/{userId}/cart-summary         — Sepet + uygulanabilir kuponlar
GET    /api/internal/agent/user/{userId}/preferences          — AI tercihleri
```

> [!CAUTION]
> Bu endpoint'ler **yalnızca iç ağdan** erişilebilir olmalıdır. `X-Internal-Key` header doğrulaması zorunludur. Spring Security'de ayrı bir `RequestMatcher` ile korunmalıdır.

---

#### [NEW] `UserAiPreferenceController.java` — AI Tercihleri

```
GET    /api/users/me/ai-preferences              — Kullanıcı AI tercihlerini getir
PUT    /api/users/me/ai-preferences              — AI tercihlerini güncelle
```

---

#### [MODIFY] `CartController.java` — Agent bayrağı ekleme

```diff
  @PostMapping("/items")
  public ResponseEntity<CartResponse> addToCart(
+     @RequestHeader(value = "X-Agent-Transaction-Id", required = false) Long agentTxId,
      @AuthenticationPrincipal JwtAuthDetails auth,
      @Valid @RequestBody AddToCartRequest req) {
+     // agentTxId varsa audit log'a AI kaynaklı olarak yaz
  }
```

---

#### [MODIFY] `OrderController.java` — Agent onay token zorunluluğu

```diff
  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
+     @RequestHeader(value = "X-Agent-Approval-Token", required = false) String approvalToken,
      @AuthenticationPrincipal JwtAuthDetails auth,
      @Valid @RequestBody CreateOrderRequest req) {
+     // approvalToken varsa agent kaynaklı sipariş — plan hash doğrulaması yapılır
  }
```

---

#### [MODIFY] `SecurityConfig.java` — Güvenlik katmanı genişletme

```diff
+ // Agent endpoint'leri için rate limiting
+ .requestMatchers("/api/agent/**").authenticated()
+ // Internal endpoint'ler için IP + key kısıtlaması
+ .requestMatchers("/api/internal/**").hasAuthority("INTERNAL_SERVICE")
```

---

#### [NEW] `AgentSecurityService.java` — Agent güvenlik servisi

```
- validateApprovalToken(token) → boolean
- calculatePlanHash(planData) → String
- verifyPlanIntegrity(token, currentPlanHash) → boolean
- checkDailyLimit(userId) → boolean (günlük limit)
- checkAmountLimit(userId, amount) → boolean (tutar limiti)
- checkHourlyLimit(userId) → boolean (saatlik limit)
- markTokenAsUsed(token) → void
- cleanupExpiredApprovals() → @Scheduled her 5 dk
```

---

### FAZ 3 — Python AI Servisi (LangGraph Genişletme)

#### [MODIFY] `graph/state.py` — AgentState genişletme

```diff
  class AgentState(TypedDict):
      messages: list[BaseMessage]
      user_id: Optional[str]
      session_id: str
      intent: Optional[str]
      selected_agent: Optional[str]
      action_type: Optional[str]
      action_data: Optional[Any]
      final_response: Optional[str]
      injection_detected: bool
      agent_type: Optional[str]
      error: Optional[str]
      current_message: str
+
+     # ── Agentic UI Control yeni alanlar ──
+     is_multi_step: bool                    # Çok adımlı işlem mi
+     requires_approval: bool                # Kullanıcı onayı gerekiyor mu
+     approval_plan: Optional[dict]          # Onay planı {steps: [...], summary: "..."}
+     approval_token: Optional[str]          # Backend'den dönen onay token'ı
+     transaction_id: Optional[int]          # Agent işlem ID'si
+     current_step: Optional[int]            # Mevcut adım numarası
+     total_steps: Optional[int]             # Toplam adım sayısı
+     completed_steps: list[dict]            # Tamamlanan adımlar [{step, status, data}]
+     rollback_actions: list[dict]           # Geri alma listesi
+     pre_validation_result: Optional[dict]  # Ön doğrulama sonucu
```

---

#### [NEW] `agents/checkout_agent.py` — Checkout Orchestration Agent

```python
"""
Kullanıcı 'siparişimi tamamla' dediğinde devreye girer.
Akış:
  1. Sepeti kontrol et (boş mu, stok var mı)
  2. Uygulanabilir kuponları sorgula, en avantajlısını öner
  3. Varsayılan adresi al
  4. Maskelenmiş ödeme yöntemini al
  5. Özet onay planı oluştur → kullanıcıya sun
  6. Onay gelirse → /api/agent/quick-checkout/execute çağır
  7. Hata olursa → detaylı açıklama ile bildir
"""

# Çok adımlı akış — her adımı SSE ile stream eder
# action_type: "APPROVAL_REQUIRED" → Frontend onay kartı gösterir
# action_type: "STEP_PROGRESS" → Frontend ilerleme göstergesi günceller
```

---

#### [NEW] `agents/navigation_agent.py` — Navigation & Status Agent

```python
"""
'Siparişlerim nerede', 'hesabıma git', 'favorilerim' gibi istekleri işler.
Sadece sayfa yönlendirmesi değil, sayfaya gidip ilgili veriye odaklanma talimatı da üretir.

Desteklenen intent'ler:
  - 'siparişlerim' → NAVIGATE /orders
  - 'profil' → NAVIGATE /profile  
  - 'favorilerim' → NAVIGATE /wishlist
  - 'sipariş durumu' → ORDER_INFO + sipariş detayı  
  - 'kargo takip' → ORDER_INFO + tracking bilgisi
"""
```

---

#### [NEW] `agents/pre_validation_agent.py` — Ön Doğrulama Agent

```python
"""
Herhangi bir destructive işlemden önce çağrılır.
Kontroller:
  - Stok yeterliliği (her üründe)
  - Kupon geçerliliği (süresi, kullanım limiti)
  - Adres eksiksizliği (zorunlu alanlar)
  - Kullanıcı limitleri (günlük, saatlik)
  - Sepet tutarı limiti

Sonuç: {valid: bool, issues: [...], warnings: [...]}
"""
```

---

#### [NEW] `agents/multi_step_executor.py` — Çok Adımlı İşlem Yürütücü

```python
"""
Birden fazla API çağrısını sıralı yürüten motor.
Her adımı:
  1. Başlatmadan önce JWT geçerliliğini kontrol eder
  2. Adımı çalıştırır
  3. Sonucu kaydeder (agent_transaction_steps tablosuna)
  4. SSE ile frontend'e ilerleme bildirir
  5. Hata olursa rollback listesini ters sırada çalıştırır

Rollback mantığı:
  - Sepete ekleme → sepetten çıkarma (rollback edilebilir)
  - Kupon uygulama → kuponu kaldırma (rollback edilebilir)
  - Sipariş oluşturma → sipariş iptali (koşullu — PENDING ise rollback edilebilir)
  - Stok düşürme → stok geri yükleme (sipariş iptalinde otomatik)
"""
```

---

#### [MODIFY] `agents/supervisor.py` — Yeni intent'ler

```diff
  INTENT_TO_AGENT: dict[str, str] = {
      IntentType.PRODUCT_FILTER: "filter_agent",
      IntentType.CART_ACTION: "cart_agent",
      IntentType.RECOMMENDATION: "recommend_agent",
      IntentType.ORDER_QUERY: "order_agent",
      IntentType.FAQ: "faq_agent",
      IntentType.GENERAL: "supervisor",
+     IntentType.CHECKOUT_FLOW: "checkout_agent",
+     IntentType.NAVIGATE_TO: "navigation_agent",
  }
```

Supervisor system prompt'una yeni intent tanımları eklenir:
```
CHECKOUT_FLOW — Sipariş tamamlama, hızlı checkout
  Örnekler: "siparişimi tamamla", "satın al", "checkout yap"

NAVIGATE_TO — Sayfa yönlendirme
  Örnekler: "siparişlerime git", "profilimi göster", "favorilerim"
```

---

#### [MODIFY] `agents/cart_agent.py` — Güvenlik ve onay eklentileri

```diff
  # ADD aksiyonunda:
+ # 1. find_cheapest kullanıldığında önce "Bu ürünü ekleyeyim mi?" onay planı sun
+ # 2. Ekleme sonrası uygulanabilir kupon varsa kullanıcıya öner
+ # 3. Sepet tutarı eşik değeri aşarsa kullanıcıyı bilgilendir

  # CLEAR aksiyonunda:
+ # Mevcut: doğrudan temizliyor — YANLIŞ
+ # Düzeltme: Önce onay planı oluştur, onay gelirse temizle
```

---

#### [MODIFY] `graph/agent_graph.py` — Yeni node'lar ve akış

```diff
  graph.add_node("supervisor_node", supervisor_node)
  graph.add_node("filter_agent", filter_agent_node)
  graph.add_node("cart_agent", cart_agent_node)
  graph.add_node("recommend_agent", recommend_agent_node)
  graph.add_node("order_agent", order_agent_node)
  graph.add_node("faq_agent", faq_agent_node)
  graph.add_node("supervisor", supervisor_respond)
+ graph.add_node("checkout_agent", checkout_agent_node)
+ graph.add_node("navigation_agent", navigation_agent_node)
+ graph.add_node("pre_validation", pre_validation_node)

  # Conditional routing güncelleme:
  graph.add_conditional_edges(
      "supervisor_node",
      route_to_agent,
      {
          "filter_agent": "filter_agent",
          "cart_agent": "cart_agent",
          "recommend_agent": "recommend_agent",
          "order_agent": "order_agent",
          "faq_agent": "faq_agent",
          "supervisor": "supervisor",
+         "checkout_agent": "checkout_agent",
+         "navigation_agent": "navigation_agent",
      },
  )

+ # Checkout agent → pre_validation → END (çok adımlı akış)
+ graph.add_edge("checkout_agent", END)
+ graph.add_edge("navigation_agent", END)
```

---

#### [NEW] `tools/coupon_tools.py` — Kupon araç seti

```python
# get_applicable_coupons(user_id, cart_summary) → kupon listesi
# dry_run_coupon(user_id, coupon_code) → simülasyon sonucu
```

---

#### [NEW] `tools/approval_tools.py` — Onay araç seti

```python
# create_approval(user_id, plan_data) → {approval_token, expires_at}
# check_approval_status(token) → {status, remaining_seconds}
```

---

#### [NEW] `tools/address_tools.py` — Adres araç seti

```python
# get_default_address(user_id) → adres bilgisi
# get_payment_methods(user_id) → maskelenmiş ödeme yöntemleri
```

---

### FAZ 4 — Frontend (Angular 19)

#### [NEW] `shared/components/agent-approval-card/` — Onay Kartı Bileşeni

```
Chatbot içinde açılan özel UI bloğu:
- "Şu işlemi yapacağım" başlığı
- Adım adım liste (her adımın açıklaması)
- Onay/Red butonları  
- Geri sayım timer (10 dk → 0)
- "Hayır" durumunda geri alınacak adımları gösterir
- action_type: APPROVAL_REQUIRED olduğunda render edilir
```

---

#### [NEW] `shared/components/agent-progress/` — İlerleme Göstergesi

```
İşlem başladıktan sonra chatbot içinde:
- ✓ Sepet kontrol edildi
- ✓ Kupon uygulandı  
- ⏳ Sipariş oluşturuluyor...
- ○ Onay bekleniyor

Her adım SSE ile güncellenir.
action_type: STEP_PROGRESS olduğunda render edilir.
```

---

#### [NEW] `features/profile/ai-history/` — Agent İşlem Geçmişi

```
Profil sayfasında "AI İşlemlerim" sekmesi:
- GET /api/agent/transactions ile veri çeker
- Her işlem: zaman, tip, adım sayısı, durum badge, sipariş linki
- Başarılı/Başarısız filtresi
```

---

#### [NEW] `features/profile/ai-preferences/` — AI Tercihleri Sayfası

```
Kullanıcı AI tercihlerini yönetir:
- Otomatik onay açık/kapalı toggle
- Otomatik onay maks tutarı (input)
- Otomatik onay kategorileri (checkbox listesi)
- Varsayılan adres kullanılsın mı toggle
- Günlük işlem limiti (readonly gösterim)
```

---

#### [NEW] `shared/components/agent-notification-banner/` — Onay Bildirimi

```
AI bir işlem planı hazırladığında chatbot dışında da görünür:
- Ekranın üst kısmında slide-down banner
- "AI bir işlem hazırladı, onaylamak ister misiniz?" mesajı
- Tıklanınca chatbot açılır ve onay kartına scroll eder
- Chatbot kapalıyken de görünür
- 10 dk sonra otomatik kaybolur
```

---

#### [NEW] `core/services/agent-bridge.service.ts` — Agent Köprü Servisi

```typescript
@Injectable({ providedIn: 'root' })
export class AgentBridgeService {
  // AI'dan gelen aksiyon tipine göre uygun Angular servisini çağırır
  
  // Yeni action type'lar:
  // APPROVAL_REQUIRED → onay kartı render et
  // STEP_PROGRESS → ilerleme göstergesi güncelle
  // CHECKOUT_COMPLETE → sipariş onay sayfasına yönlendir
  // AI_FEEDBACK_REQUEST → geri bildirim dialogu göster
  
  readonly pendingApproval$ = signal<PendingApproval | null>(null);
  readonly transactionProgress$ = signal<TransactionProgress | null>(null);
  readonly notificationBanner$ = signal<boolean>(false);
}
```

---

#### [NEW] `core/services/approval.service.ts` — Onay Yönetim Servisi

```typescript
@Injectable({ providedIn: 'root' })
export class ApprovalService {
  approveTransaction(token: string): Observable<void>;
  rejectTransaction(token: string): Observable<void>;
  checkApprovalStatus(token: string): Observable<ApprovalStatus>;
  readonly countdownSeconds$ = signal<number>(0);
  // Onay süresi dolmadan hatırlatma tetikler
}
```

---

#### [MODIFY] `core/services/ai-chat.service.ts` — Genişletme

```diff
  handleAgentAction(res: any): void {
    if (!res.actionType || !res.actionData) return;

    switch (res.actionType) {
      case 'PRODUCT_LIST': { break; }
      case 'CART_UPDATED': { this.cartService.getCart().subscribe(); break; }
      case 'NAVIGATE': { /* ... */ break; }
      case 'ORDER_INFO': { break; }
+     case 'APPROVAL_REQUIRED': {
+       this.agentBridge.pendingApproval$.set(res.actionData);
+       this.agentBridge.notificationBanner$.set(true);
+       break;
+     }
+     case 'STEP_PROGRESS': {
+       this.agentBridge.transactionProgress$.set(res.actionData);
+       break;
+     }
+     case 'CHECKOUT_COMPLETE': {
+       this.router.navigate(['/orders/confirmation', res.actionData.orderNumber]);
+       this.cartService.getCart().subscribe();
+       break;
+     }
+     case 'AI_FEEDBACK_REQUEST': {
+       // Geri bildirim dialogu
+       break;
+     }
    }
  }
```

---

#### [MODIFY] `core/models/product.model.ts` — Yeni tipler

```diff
- export type AgentActionType = 'PRODUCT_LIST' | 'CART_UPDATED' | 'NAVIGATE' | 'INFO' | 'ORDER_INFO' | 'ERROR';
+ export type AgentActionType = 
+   'PRODUCT_LIST' | 'CART_UPDATED' | 'NAVIGATE' | 'INFO' | 'ORDER_INFO' | 'ERROR' |
+   'APPROVAL_REQUIRED' | 'STEP_PROGRESS' | 'CHECKOUT_COMPLETE' | 'AI_FEEDBACK_REQUEST';

+ export interface PendingApproval {
+   approvalToken: string;
+   steps: ApprovalStep[];
+   summary: string;
+   expiresAt: string;
+   estimatedDuration: string;
+ }
+
+ export interface ApprovalStep {
+   order: number;
+   type: string;
+   description: string;
+   isRollbackable: boolean;
+ }
+
+ export interface TransactionProgress {
+   transactionId: number;
+   currentStep: number;
+   totalSteps: number;
+   steps: TransactionStepStatus[];
+ }
+
+ export interface TransactionStepStatus {
+   order: number;
+   type: string;
+   description: string;
+   status: 'pending' | 'in_progress' | 'completed' | 'failed';
+ }
+
+ export interface UserAiPreference {
+   autoApproveEnabled: boolean;
+   autoApproveMaxAmount: number | null;
+   autoApproveCategories: string[];
+   useDefaultAddress: boolean;
+   useDefaultPayment: boolean;
+   dailyTransactionLimit: number;
+   maxOrderAmount: number;
+ }
+
+ export interface AgentTransaction {
+   id: number;
+   transactionType: string;
+   status: string;
+   totalSteps: number;
+   completedSteps: number;
+   createdAt: string;
+   orderNumber?: string; // Varsa oluşturulan sipariş
+ }
```

---

#### [MODIFY] `shared/components/chatbot/` — Chatbot genişletme

```diff
+ // Yeni mesaj render tipleri:
+ // 1. Normal mesaj baloncuğu (mevcut)
+ // 2. Onay kartı (APPROVAL_REQUIRED) → AgentApprovalCardComponent
+ // 3. İlerleme göstergesi (STEP_PROGRESS) → AgentProgressComponent
+
+ // İşlem sırasında input disable:
+ // transactionProgress$ aktifken mesaj input'u disabled olur
+ // "İşlemi İptal Et" butonu görünür
+
+ // İşlem süre tahmini:
+ // "Bu işlem yaklaşık 10-15 saniye sürebilir" mesajı
```

---

#### [MODIFY] `core/services/auth.service.ts` — Session koruma

```diff
+ // Agent işlemi sırasında session kontrolü:
+ // Her 30 saniyede bir /api/auth/me çağrısı ile session'ın aktif olduğunu doğrula
+ // Session doluyorsa otomatik refresh token yenileme
+ // Session sona ererse işlemi askıya al, login sonrası kaldığı yerden devam
```

---

### FAZ 5 — Güvenlik Katmanı (Tüm Servisleri Kesen)

#### Onay Mekanizması Güvenliği

| Kural | Uygulama Yeri | Detay |
|-------|---------------|-------|
| Her destructive işlem onay gerektirir | `AgentSecurityService` | Sepet temizleme, sipariş oluşturma, adres değiştirme |
| Token tek kullanımlık | `PendingApproval.status` | APPROVED olduktan sonra tekrar kullanılamaz |
| Token 10 dk ömürlü | `PendingApproval.expiresAt` | `NOW() + 10 minutes` |
| Plan hash bütünlüğü | `AgentSecurityService.verifyPlanIntegrity()` | SHA-256 hash karşılaştırması |
| İşlem başına maks tutar | `UserAiPreference.maxOrderAmount` | Varsayılan: 5000 TL |
| Günlük maks işlem | `UserAiPreference.dailyTransactionLimit` | Varsayılan: 10 |
| Saatlik maks işlem | `AgentSecurityService.checkHourlyLimit()` | Varsayılan: 5 |
| Spam koruması | `RateLimitingFilter` genişletme | Aynı ürünü 1 dk içinde tekrar ekleyemez |

#### Prompt Injection — Ek Katman

```
Mevcut 3 katmana ek:
- Çok adımlı akışlarda HER ADIMDAN ÖNCE tekrar injection kontrolü
- "Adresi şu kişiye gönder" → kayıtlı adres dışı reddedilir
- "Şu kuponu uygula" → kupon gerçekten geçerli mi backend'den doğrulanır
- Adres değişikliği serbest metin kabul etmez, yalnızca kayıtlı adres ID
```

#### Veri İzolasyonu

```
- Agent plan oluştururken yalnızca o kullanıcının verileri
- Ödeme bilgileri AI servisine ASLA açık iletilmez (son 4 hane mask)
- Kullanıcı kimliği 2x doğrulama: plan oluşturma + sipariş oluşturma
- Agent'lar yalnızca kendi tool setlerini kullanır (supervisor üzerinden)
```

---

### FAZ 6 — Güvenlik Test Senaryoları (Section 11'e Ek)

> Mevcut 15 test senaryosuna ek olarak:

| # | Test | Beklenen |
|---|------|----------|
| 16 | Onay token'ını tekrar kullanma | 403 TOKEN_ALREADY_USED |
| 17 | Süresi dolmuş token ile işlem tetikleme | 403 TOKEN_EXPIRED |
| 18 | Onaylanan planı değiştirip farklı sipariş oluşturma | 403 PLAN_TAMPERED |
| 19 | Agent ile başka kullanıcının adresine sipariş | 403 ADDRESS_NOT_OWNED |
| 20 | Günlük işlem limitini aşma | 429 DAILY_LIMIT_EXCEEDED |
| 21 | İşlem sırasında session sonlandırma | İşlem askıya alınır, login sonrası devam |
| 22 | Onay ekranını atlayıp doğrudan quick-checkout | 403 APPROVAL_REQUIRED |
| 23 | Chat mesajına sahte onay token'ı yerleştirme | Token DB'de bulunamaz → 404 |

---

### FAZ 7 — Kullanıcı Deneyimi Eklentileri

#### [NEW] AI Hoşgeldin Ekranı
- Kullanıcı ilk kez chatbot'tan bir AI-tetikli işlem yaptığında gösterilir
- "AI Asistanınız neler yapabilir?" başlığı
- 4-5 kısa özellik kartı (sipariş tamamlama, kupon önerme, navigasyon, vb.)
- "Anladım, başlayalım" butonu — localStorage'a `ai_onboarded: true` yazılır

#### [NEW] AI Geri Bildirim
- Her agent işlemi sonrası: "Bu işlem yardımcı oldu mu?" 👍/👎
- Olumsuz geri bildirimde opsiyonel açıklama textarea'sı
- Geri bildirim `agent_transactions` tablosuna `feedback_score` ve `feedback_text` olarak kaydedilir

#### [NEW] İşlem Süre Tahmini
- Çok adımlı işlem başlamadan önce: "Bu işlem yaklaşık X saniye sürecektir"
- Beklenti yönetimi → kullanıcı sabırsızlanmaz

#### [NEW] Hata Bildirme Butonu
- Chat içinden "Bu beklenmedik bir davranış" butonu
- Tıklanınca son 5 mesajı ve agent durumunu otomatik yakalar
- Backend'e `POST /api/ai/feedback/bug-report` ile gönderir

---

## Implementation Sırası (Önerilen)

> [!TIP]
> todo2.md'nin önerdiği sıra doğru ama detaylandırılmalı:

```
FAZ 1 (Hafta 1) — Veritabanı + Backend Güvenlik Temeli
├── 1.1 Yeni entity'ler oluştur (4 yeni entity)
├── 1.2 Mevcut entity'lere alanlar ekle (Order, Cart, AuditLog, AiConversation)
├── 1.3 Repository'ler oluştur
├── 1.4 AgentSecurityService yaz (token üretme, hash, limit kontrol)
├── 1.5 AgentApprovalController + Service yaz
├── 1.6 QuickCheckoutController + Service yaz

FAZ 2 (Hafta 1-2) — Frontend Onay Kartı + Köprü Servisi
├── 2.1 Yeni TypeScript interface'leri ekle
├── 2.2 AgentBridgeService yaz
├── 2.3 ApprovalService yaz
├── 2.4 AgentApprovalCardComponent yaz
├── 2.5 AgentProgressComponent yaz
├── 2.6 Chatbot'u genişlet (yeni render tipleri)
├── 2.7 AgentNotificationBannerComponent yaz

FAZ 3 (Hafta 2) — Python Checkout Agent
├── 3.1 AgentState genişlet
├── 3.2 Yeni tool'lar yaz (coupon, approval, address)
├── 3.3 checkout_agent.py yaz — TEK ADIMLI akışlarla test et
├── 3.4 navigation_agent.py yaz
├── 3.5 Supervisor'a yeni intent'leri ekle
├── 3.6 Graph'a yeni node'lar ekle

FAZ 4 (Hafta 2-3) — Çok Adımlı Akışlar + Rollback
├── 4.1 multi_step_executor.py yaz
├── 4.2 pre_validation_agent.py yaz
├── 4.3 Cart agent'a onay adımı ekle
├── 4.4 SSE ile adım adım ilerleme streaming
├── 4.5 Rollback mekanizmasını test et

FAZ 5 (Hafta 3) — Limitler + Audit + AI Tercihler
├── 5.1 AgentInternalController yaz
├── 5.2 UserAiPreferenceController + Component yaz
├── 5.3 Agent işlem geçmişi sayfası yaz
├── 5.4 Audit log genişletme (isAiAction, transactionId)
├── 5.5 Rate limiting genişletme

FAZ 6 (Hafta 3-4) — UX Eklentileri + Güvenlik Testleri
├── 6.1 AI hoşgeldin ekranı
├── 6.2 Geri bildirim mekanizması
├── 6.3 Hata bildirme butonu
├── 6.4 Güvenlik testlerini çalıştır (Test 16-23)
├── 6.5 End-to-end tam akış testi
```

---

## Verification Plan

### Automated Tests
- Backend unit test: `AgentSecurityServiceTest` — token üretme, hash, limit kontrol
- Backend integration test: `QuickCheckoutIntegrationTest` — tam checkout akışı
- Backend test: `ApprovalControllerTest` — onay/red/timeout senaryoları
- Frontend unit test: `AgentBridgeService` ve `ApprovalService` mock testleri

### Manual Verification
1. **Happy path testi**: Chatbot'a "siparişimi tamamla" → onay kartı görünür → onay ver → sipariş oluşur → confirmation sayfası
2. **Timeout testi**: Onay kartı açık bırak → 10 dk bekle → token expired mesajı
3. **Rollback testi**: Checkout akışında stok bitsin → önceki adımlar geri alınsın → kullanıcıya açıklama
4. **Güvenlik testi**: Test 16-23 senaryolarını Postman ile çalıştır
5. **UX testi**: Mobilde chatbot + onay kartı + ilerleme göstergesi görsel doğrulama

### Browser Testing
- Chatbot'tan tam checkout akışı kaydı
- Onay kartı geri sayım animasyonu
- Çok adımlı işlem ilerleme göstergesi
- Hata durumunda rollback bildirimi
