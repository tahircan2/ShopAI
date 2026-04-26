"""
agents/analytics_agent.py — Text2SQL + Analysis + Visualization Agent.

Üç aşamalı pipeline:
  1. SQL Generation  — Doğal dil → güvenli READ-ONLY SQL
  2. Analysis         — SQL sonuçlarını yorumla
  3. Visualization    — Chart.js uyumlu grafik config üret

Güvenlik:
  - SADECE SELECT sorguları kabul edilir (INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE bloklanır)
  - Spring Boot backend'in /api/internal/analytics/query endpoint'i üzerinden çalışır
  - user_role kontrolü yapılır — sadece ADMIN ve SELLER analytics kullanabilir
"""

import re
import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState

logger = structlog.get_logger(__name__)

# ─── Veritabanı Şema Referansı (LLM'e verilecek) ────────────────────────────
DB_SCHEMA = """
Veritabanı Tabloları (MySQL):

1. users (id, email, first_name, last_name, role, created_at, is_active)
   - role: ROLE_ADMIN, ROLE_SELLER, ROLE_USER

2. products (id, name, slug, description, price, discounted_price, stock_quantity, is_active,
   seller_id → users.id, category_id → categories.id, rating_avg, rating_count, created_at)

3. categories (id, name, slug, description, parent_id → categories.id)
   - Kategori bazlı satış analizi için bu tabloyu kullanın.

4. orders (id, order_number, user_id → users.id, total_amount, status, created_at, updated_at, shipping_mode)
   - status: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED
   - shipping_mode: Air, Road, Ship

5. order_items (id, order_id → orders.id, product_id → products.id, quantity, unit_price, total_price)

6. reviews (id, product_id → products.id, user_id → users.id, rating, title, comment, created_at)
   - Puan dağılımı ve müşteri memnuniyeti analizi için bu tabloyu kullanın.

7. cart_items (id, cart_id → carts.id, product_id → products.id, quantity, added_at)

8. coupons (id, code, discount_type, discount_value, min_order_amount, max_uses, used_count,
   valid_from, valid_until, is_active)

9. audit_logs (id, user_id, action, entity_type, entity_id, is_ai_action, created_at)
"""

# ─── SQL Güvenlik Kontrolleri ─────────────────────────────────────────────────
DANGEROUS_KEYWORDS = [
    r"\bINSERT\b", r"\bUPDATE\b", r"\bDELETE\b", r"\bDROP\b",
    r"\bALTER\b", r"\bTRUNCATE\b", r"\bCREATE\b", r"\bGRANT\b",
    r"\bREVOKE\b", r"\bEXEC\b", r"\bEXECUTE\b", r"\bINTO\s+OUTFILE\b",
    r"\bLOAD\s+DATA\b", r"\bSHOW\s+GRANTS\b",
]


def sanitize_sql(sql: str) -> str | None:
    """
    SQL sorgusunu güvenlik kontrolünden geçirir.
    Sadece SELECT sorgularına izin verir.
    Returns: sanitize edilmiş SQL veya None (tehlikeli ise).
    """
    if not sql or not sql.strip():
        return None

    cleaned = sql.strip().rstrip(";")

    # Tehlikeli keyword kontrolü
    for pattern in DANGEROUS_KEYWORDS:
        if re.search(pattern, cleaned, re.IGNORECASE):
            logger.warning("dangerous_sql_blocked", sql=cleaned[:100], pattern=pattern)
            return None

    # SELECT ile başlamalı
    if not re.match(r"^\s*SELECT\b", cleaned, re.IGNORECASE):
        logger.warning("non_select_sql_blocked", sql=cleaned[:100])
        return None

    # LIMIT yoksa ekle (performans koruması)
    if not re.search(r"\bLIMIT\b", cleaned, re.IGNORECASE):
        cleaned += " LIMIT 100"

    return cleaned


# ─── SQL Generation Prompt ────────────────────────────────────────────────────
SQL_GEN_PROMPT = f"""Sen bir veritabanı uzmanısın. Kullanıcının doğal dildeki sorusunu MySQL SQL sorgusuna çevir.

{DB_SCHEMA}

KURALLAR:
1. SADECE SELECT sorguları üret. INSERT/UPDATE/DELETE/DROP YASAK.
2. Sonuçları anlamlı alias'larla döndür (AS kullanarak). Örn: SELECT SUM(total) AS toplam_gelir
3. ÖNEMLİ (Agrega): Eğer bir aggregate fonksiyonu (SUM, AVG, COUNT, MAX, MIN) kullanıyorsan ve yanında başka bir sütun seçiyorsan, MUTLAKA o sütunu GROUP BY kısmına ekle.
4. ÖNEMLİ (Hizalama): GROUP BY kısmında ALIAS değil, orijinal tablo.sütun isimlerini kullan. ORDER BY kısmında ise ALIAS kullanabilirsin.
   Örnek: SELECT p.name AS urun_adi, COUNT(*) FROM products p GROUP BY p.name ORDER BY urun_adi
5. Tarih filtreleri için CURDATE(), DATE_SUB(), YEAR(), MONTH() fonksiyonlarını kullan.
6. Performans için her zaman LIMIT ekle (max 100).
7. JOIN kullanırken alias ver (o, p, u, c, oi, pr gibi).
8. Hassas verileri (email, password_hash) ASLA döndürme.
9. SADECE SQL sorgusunu döndür, başka açıklama ekleme.
10. SQL sorgusunda '?' veya parametre kullanma. Değerleri doğrudan SQL içine yaz.
11. Tablo ve sütun isimlerini tam olarak belirtilen şemadaki gibi kullan.
12. Yetkilendirme Kuralları:
- ROLE_ADMIN: Tüm verilere erişim
- ROLE_SELLER: Sadece kendi ürünleri (seller_id = ?)
- ROLE_USER: Analytics erişimi YOK

13. Sıralama ve Limit: "En çok...", "En az...", "En iyi..." gibi sorularda sadece 1 sonuç değil, karşılaştırma yapılabilmesi için her zaman TOP 5 veya TOP 10 sonuç getirmeye çalışın (LIMIT 5 veya LIMIT 10 kullanın).
"""

# ─── Analysis Prompt ──────────────────────────────────────────────────────────
ANALYSIS_PROMPT = """Siz ShopAI'nın uzman veri analistisiniz. Aşağıdaki SQL sorgusu sonucunda elde edilen verileri profesyonel ve üst düzey bir e-ticaret danışmanı diliyle yorumlayın.

Çalıştırılan Sorgu: {sql}
Elde Edilen Veri Kümesi: {results}

KRİTİK KURALLAR:
1. **Teknik Terim YASAK**: Yanıtınızda KESİNLİKLE "SQL", "sorgu", "veritabanı", "id", "user_id", "seller_id", "null", "empty" gibi teknik terimler kullanmayın. Kullanıcıya bir yazılımcı gibi değil, bir iş ortağı gibi hitap edin.
2. **Kısalık ve Netlik**: Yanıtınız öz ve vurucu olsun. Kullanıcı sormadığı sürece gereksiz detaylara girmeyin. Maksimum 2-3 kısa paragraf.
3. **Doğal Dil**: Eğer veri yoksa (sonuç boşsa), bunu "Sistemde veri bulunamadı" veya "Sorgu sonucu boş" yerine "Henüz bu alanda bir etkileşim veya kayıt bulunmuyor" gibi doğal bir dille açıklayın.
4. **Profesyonel Hitap**: Her zaman 'Siz' dilini kullanın. Modern ve nazik olun.
5. **Görsel Düzen**: Markdown başlıklarını (###) ve kalın yazıları (**) sadece en kritik yerlerde kullanın.
6. **Aksiyon Odaklılık**: Veriyi sadece raporlamayın, çok kısa bir stratejik ipucu ekleyin.
7. **Kapsam Farkındalığı**: Eğer veri kümesinde az sayıda sonuç varsa, bunun veritabanındaki TÜM veriler olduğunu varsaymayın. Sorgunun (sql) bir kısıtlama (WHERE, LIMIT, TOP vb.) içerip içermediğine bakın. Örneğin; "En çok yorum alan ürün hangisi?" sorusuna tek bir sonuç geldiyse, "Sistemde sadece bir ürün var" demek yerine "En çok ilgi gören ürününüz şudur" şeklinde yanıt verin.

Örnek Yanıt Yapısı:
### 📊 Durum Analizi
(Verinin özeti)
### 💡 Öneri
(Tek cümlelik aksiyon önerisi)"""

# ─── Visualization Prompt ─────────────────────────────────────────────────────
VIZ_PROMPT = """Aşağıdaki SQL sonuçları için Chart.js uyumlu grafik konfigürasyonu üret.

Sorgu: {sql}
Sonuçlar: {results}

Sadece JSON formatında döndür (başka açıklama yazma). Format:
{{
  "type": "bar|line|doughnut|pie",
  "data": {{
    "labels": ["...", "..."],
    "datasets": [{{
      "label": "...",
      "data": [1, 2, 3],
      "backgroundColor": ["#6366f1", "#22d3ee", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#14b8a6", "#f97316", "#06b6d4"],
      "borderColor": "#ffffff",
      "borderWidth": 1
    }}]
  }},
  "options": {{
    "responsive": true,
    "plugins": {{ "legend": {{ "position": "top" }} }}
  }}
}}

GÖREV: En uygun grafik tipini aşağıdaki kurallara göre SEÇ (type alanına yaz):
1. İçinde "kategori", "kategoriye göre", "puan", "statü", "oran", "dağılım" geçen sorgular veya gruplamalar -> KESİNLİKLE "pie" veya "doughnut" KULLAN.
2. Zaman serisi (aylar, yıllar, günler, tarihler) -> KESİNLİKLE "line" KULLAN.
3. Ürün bazlı satış sıralaması, en çok satan ürünler veya ürün karşılaştırmaları -> KESİNLİKLE "bar" KULLAN (Yatay bar istersen options içine indexAxis: 'y' ekle, ama type HER ZAMAN "bar" olsun).
"""


async def analytics_agent_node(state: AgentState) -> AgentState:
    """
    Text2SQL analytics agent — 3 aşamalı pipeline:
    1. SQL Generation
    2. Query Execution (via Spring Boot)
    3. Analysis + Visualization
    """
    user_role = state.get("user_role", "")
    user_id = state.get("user_id")
    message = state.get("current_message", "")
    messages = state["messages"]

    logger.info("analytics_agent_check", user_id=user_id, user_role=user_role)

    # Rol kontrolü
    if not user_role or user_role.strip() == "" or user_role.lower() == "none":
        logger.warning("analytics_access_denied_no_role", user_id=user_id)
        return {
            **state,
            "final_response": (
                "📊 **Analytics özelliklerine şu anda erişemiyorum.**\n\n"
                "Güvenlik protokolleri gereği bu verilere erişmek için giriş yapmış olmanız gerekmektedir. "
                "Lütfen **sayfayı yenileyerek** tekrar deneyin."
            ),
            "agent_type": "analytics_agent",
            "action_type": "INFO",
        }

    if user_role not in ("ROLE_ADMIN", "ROLE_SELLER", "ROLE_USER", "ADMIN", "SELLER", "USER"):
        logger.warning("analytics_access_denied_wrong_role", user_id=user_id, role=user_role)
        return {
            **state,
            "final_response": "📊 Analytics özellikleri mevcut yetkiniz ile kullanılamaz.",
            "agent_type": "analytics_agent",
            "action_type": "INFO",
        }

    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        max_tokens=1024,
        api_key=settings.openai_api_key,
    )

    # ── Aşama 1: SQL Generation ──────────────────────────────────────────────
    try:
        # Rol bazlı kısıtlama ekle
        role_ctx = ""
        if user_role in ("ROLE_SELLER", "SELLER"):
            role_ctx = (
                f"\nKullanıcı bir SATICI (id = {user_id}).\n"
                f"DİL KURALLARI (ÖNEMLİ):\n"
                f"- Eğer kullanıcı 'ürünüm', 'benim', 'mağazam', 'satışlarım', 'kazancım' gibi iyelik/sahiplik belirten ifadeler kullanıyorsa (Örn: 'en çok yorum alan ürünüm hangisi?'): "
                f"SADECE kendi verilerini görmeli. Bu durumda MUTLAKA 'products.seller_id = {user_id}' filtresini kullan.\n"
                f"- Eğer kullanıcı genel ifadeler kullanıyorsa (Örn: 'en çok yorum alan ürün hangisi?', 'en popüler kategoriler') veya global bir trend soruyorsa: "
                f"Tüm platform verilerine erişebilir. Bu durumda seller_id filtresi EKLEME.\n\n"
                f"DİKKAT: 'orders' tablosundaki 'user_id' MÜŞTERİYİ temsil eder. Kendi satışlarını görmek için "
                f"MUTLAKA 'order_items' ve 'products' tablolarını joinle ve 'products.seller_id = {user_id}' filtresini kullan.\n"
                f"'?' kullanma, doğrudan {user_id} yaz."
            )
        elif user_role in ("ROLE_ADMIN", "ADMIN"):
            role_ctx = "\nKullanıcı bir YÖNETİCİ. Tüm platform verilerine erişebilir.\n"
        elif user_role in ("ROLE_USER", "USER"):
            role_ctx = (
                "\nKullanıcı bir MÜŞTERİ (ROLE_USER).\n"
                "Müşteriler platformdaki genel ürün trendlerini sorgulayabilir.\n"
                "GÜVENLİK (ÇOK ÖNEMLİ): Müşteriler ASLA ciro (total_amount), mağaza kazançları, diğer müşterilerin özel bilgileri veya sipariş detaylarını GÖREMEZ.\n"
            )

        sql_hints = (
            "\nSQL İPUÇLARI (DOĞRULUK İÇİN):\n"
            "- 'en çok satan ürün': order_items ve products tablolarını joinle, SUM(order_items.quantity) AS total_qty üzerinden grupla, ORDER BY total_qty DESC yap.\n"
            "- 'en çok yorum alan / değerlendirilen ürün': products.rating_count sütununu kullan (ORDER BY rating_count DESC) veya reviews tablosunu joinleyip COUNT(*) kullan.\n"
            "- 'en yüksek puan alan ürün': products.rating_avg sütununu kullan (ORDER BY rating_avg DESC). Sadece rating_count > 0 olanları filtrele ki hiç yorum almayanlar 0 veya NULL gelmesin.\n"
            "- 'en yüksek/pahalı ürün': ORDER BY products.price DESC\n"
            "- 'en düşük/ucuz ürün': ORDER BY products.price ASC\n"
            "Tüm bu genel/trend sorgularında her zaman LIMIT 5 veya 10 kullanarak en iyi ürünleri getir."
        )

        role_ctx += sql_hints

        messages_to_send = [
            SystemMessage(content=SQL_GEN_PROMPT),
            SystemMessage(content=role_ctx),
            *messages[-10:],  # Son 10 mesaj context için
        ]
        sql_response = await llm.ainvoke(messages_to_send)

        raw_sql = sql_response.content.strip()
        # Markdown code block varsa temizle
        if raw_sql.startswith("```"):
            raw_sql = re.sub(r"```(?:sql)?\s*", "", raw_sql).strip().rstrip("```").strip()

        safe_sql = sanitize_sql(raw_sql)
        if not safe_sql:
            logger.warning("sql_generation_blocked", raw_sql=raw_sql[:200])
            return {
                **state,
                "final_response": "⚠️ Bu sorguyu güvenlik nedeniyle çalıştıramıyorum. Lütfen farklı bir analiz sorusu sorun.",
                "agent_type": "analytics_agent",
                "action_type": "INFO",
            }

        logger.info("sql_generated", sql=safe_sql[:200])

    except Exception as e:
        logger.error("sql_generation_error", error=str(e))
        return {
            **state,
            "final_response": "SQL sorgusu oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "analytics_agent",
            "error": str(e),
        }

    # ── Aşama 2: Query Execution ─────────────────────────────────────────────
    if not safe_sql:
        logger.error("sql_blocked_or_invalid", raw_sql=raw_sql)
        return {
            "messages": [
                {"role": "assistant", "content": "Üzgünüm, bu soru için güvenli bir sorgu oluşturamadım."}
            ],
            "current_agent": "supervisor"
        }

    try:
        import httpx
        async with httpx.AsyncClient(timeout=30.0) as client:
            logger.info("sending_analytics_request", user_id=user_id, sql_preview=safe_sql[:100])
            resp = await client.post(
                f"{settings.spring_boot_base_url}/internal/analytics/query",
                json={
                    "sql": safe_sql,
                    "user_id": user_id
                },
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key
                },
            )

        if resp.status_code != 200:
            error_body = resp.text[:200]
            logger.error("analytics_query_failed", status=resp.status_code, body=error_body)
            return {
                **state,
                "final_response": f"⚠️ Sorgu çalıştırılamadı (HTTP {resp.status_code}). Lütfen farklı bir soru deneyin.",
                "agent_type": "analytics_agent",
                "action_type": "INFO",
            }

        results = resp.json()
        if not isinstance(results, list):
            results = results.get("data", [])

        logger.info("query_executed", row_count=len(results))

    except Exception as e:
        logger.error("analytics_query_error", error=str(e))
        return {
            **state,
            "final_response": "Veritabanı sorgusu çalıştırılırken bir hata oluştu.",
            "agent_type": "analytics_agent",
            "error": str(e),
        }

    # ── Aşama 3a: Analysis ───────────────────────────────────────────────────
    results_preview = json.dumps(results[:20], ensure_ascii=False, default=str)

    try:
        analysis_llm = ChatOpenAI(
            model=settings.openai_model,
            temperature=0.3,
            api_key=settings.openai_api_key,
            tags=["stream_to_user"]
        )
        analysis_response = await analysis_llm.ainvoke([
            SystemMessage(content=ANALYSIS_PROMPT.format(sql=safe_sql, results=results_preview)),
            HumanMessage(content=message),
        ])
        analysis_text = analysis_response.content.strip()
    except Exception as e:
        logger.error("analysis_error", error=str(e))
        analysis_text = "Analiz oluşturulurken bir hata oluştu."

    # ── Aşama 3b: Visualization ──────────────────────────────────────────────
    chart_config = None
    if len(results) > 0:
        try:
            viz_llm = ChatOpenAI(
                model=settings.openai_model,
                temperature=0,
                api_key=settings.openai_api_key,
            )
            viz_response = await viz_llm.ainvoke([
                SystemMessage(content=VIZ_PROMPT.format(sql=safe_sql, results=results_preview)),
            ])

            viz_raw = viz_response.content.strip()
            # JSON parse
            if viz_raw.startswith("```"):
                viz_raw = re.sub(r"```(?:json)?\s*", "", viz_raw).strip().rstrip("```").strip()

            chart_config = json.loads(viz_raw)
            logger.info("chart_config_generated", chart_type=chart_config.get("type"))

        except (json.JSONDecodeError, Exception) as e:
            logger.warning("visualization_error", error=str(e))
            chart_config = None

    return {
        **state,
        "final_response": analysis_text,
        "agent_type": "analytics_agent",
        "action_type": "ANALYTICS_RESULT",
        "action_data": {
            "sql": safe_sql,
            "results": results[:50],  # Max 50 row frontend'e
            "chartConfig": chart_config,
            "rowCount": len(results),
        },
        "generated_sql": safe_sql,
        "sql_results": results[:50],
        "analysis_text": analysis_text,
        "chart_config": chart_config,
    }
