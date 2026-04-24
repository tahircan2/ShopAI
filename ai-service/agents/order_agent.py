"""
agents/order_agent.py — Sipariş durumu sorgulama agent'ı.

Kullanıcının sipariş durumunu, kargo takibini ve sipariş geçmişini sorgular.
Tüm sorgular JWT'den extract edilen user_id ile scope edilir.
Bir kullanıcı başka kullanıcının siparişini sorgulayamaz.

FILTER DESTEĞİ:
  - Durum filtresi: "kargoya verilen siparişlerimi göster"
  - Tarih filtresi: "22 Nisan tarihli siparişlerimi göster"
  - Kombine filtreler: "bu ay iptal edilen siparişlerim"
"""

import json
import re
from datetime import datetime, timedelta
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, BaseMessage

from config import settings
from graph.state import AgentState
from tools.order_tools import get_user_orders, get_order_detail, get_latest_order

logger = structlog.get_logger(__name__)

ORDER_INTENT_PROMPT = """Sen bir sipariş sorgu sistemisin.
Kullanıcı mesajını analiz et ve sipariş sorgusunu çıkar. JSON döndür.

JSON formatı:
{{
  "query_type": "LATEST|LIST|DETAIL|STATUS",
  "order_number": "sipariş numarası (varsa, ör. ORD-20240101-XXXX)",
  "status_filter": "PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED|REFUNDED|null",
  "date_filter": "YYYY-MM-DD veya null",
  "date_range_start": "YYYY-MM-DD veya null",
  "date_range_end": "YYYY-MM-DD veya null"
}}

Sorgular:
- LATEST: "son siparişim", "en son siparişim", "son kargom"
- LIST: "siparişlerim", "tüm siparişlerim", "sipariş geçmişim"
- DETAIL: sipariş numarası belirtilmişse (ör. ORD-20260422-9560)
- STATUS: "nerede", "ne zaman gelir", "kargo durumu"

Filtreler:
- status_filter: Kullanıcı belirli bir durum istiyorsa ata
  "beklemede/bekleyen" → PENDING
  "onaylanan/onaylanmış" → CONFIRMED
  "kargoya verilen/kargodaki" → SHIPPED
  "teslim edilen/teslim edilmiş" → DELIVERED
  "iptal edilen/iptal edilmiş" → CANCELLED
  "iade edilen" → REFUNDED
- date_filter: Belirli bir tarih istenmişse ISO formatında yaz
  "22 Nisan" → bugünün yılı ile birlikte "2026-04-22"
  "dün" → dünün tarihi
  "bugün" → bugünün tarihi
- date_range_start / date_range_end: Tarih aralığı istenmişse
  "bu hafta", "bu ay", "son 7 gün" gibi ifadeler için uygun aralıkları hesapla

Bugünün tarihi: {today}

SADECE JSON döndür. Filtre yoksa ilgili alanları null yap."""

ORDER_RESPONSE_PROMPT = """Siz ShopAI'nın profesyonel sipariş asistanısınız. 
Sana bir veya birden fazla siparişin teknik detayları (JSON formatında) ve kullanıcının mesaj geçmişi verilecek.

GÖREVİNİZ:
1. **Kurumsal ve Şık Yanıt**: Her zaman 'Siz' hitabını kullanın. Modern e-ticaret standartlarına uygun, güven veren bir dil tercih edin.
2. **Yapılandırılmış Bilgi**: Sipariş bilgilerini (No, Tarih, Durum, Tutar) Markdown başlıkları veya kalın yazılarla organize edin.
3. **Ürün Detayları Notu**: Ürün listesini (isim, görsel vb.) uzun uzun yazmayın. Arayüzde otomatik kartlar olarak gösterileceğini bildiğiniz için sadece genel sipariş kapsamından bahsedin.
4. **Durum Vurgusu**: Kargo (SHIPPED) veya Teslimat (DELIVERED) gibi kritik aşamaları emoji ve net ifadelerle ön plana çıkarın.
5. **Kısa ve Etkili**: Gereksiz cümlelerden kaçının, kullanıcının ihtiyacı olan bilgiyi saniyeler içinde almasını sağlayın.

Sipariş Verileri:
{order_data}

Örnek Yapı:
### 📦 Sipariş Özeti
- **Sipariş No:** ORD-XXXX
- **Durum:** [Emoji] [Durum Metni]
- **Tutar:** XX TL

... (Varsa diğer siparişler veya ek bilgilendirme)
"""

ORDER_STATUS_LABELS = {
    "PENDING": "Beklemede",
    "CONFIRMED": "Onaylandı",
    "SHIPPED": "Kargoya Verildi",
    "DELIVERED": "Teslim Edildi",
    "CANCELLED": "İptal Edildi",
    "REFUNDED": "İade Edildi",
}

STATUS_EMOJIS = {
    "PENDING": "⏳",
    "CONFIRMED": "✅",
    "SHIPPED": "🚚",
    "DELIVERED": "📦",
    "CANCELLED": "❌",
    "REFUNDED": "↩️",
}


def _extract_order_number(message: str) -> str | None:
    """Mesajdan sipariş numarasını çıkarır."""
    pattern = r"ORD-\d{8}-[A-Z0-9]{4}"
    match = re.search(pattern, message.upper())
    return match.group(0) if match else None


async def parse_order_intent(messages: list[BaseMessage]) -> dict:
    """Sohbet geçmişinden sipariş sorgusunu ve filtrelerini çıkarır."""
    last_message = messages[-1].content if messages else ""

    # Önce regex ile sipariş numarası ara (hızlı - sadece son mesajda)
    order_number = _extract_order_number(last_message)
    if order_number:
        return {"query_type": "DETAIL", "order_number": order_number}

    today = datetime.now().strftime("%Y-%m-%d")
    prompt_with_date = ORDER_INTENT_PROMPT.format(today=today)

    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=prompt_with_date),
            *messages,
        ])
        result = json.loads(response.content)
        logger.info("order_intent_parsed", result=result)
        return result
    except Exception as e:
        logger.error("order_intent_parse_error", error=str(e))
        return {"query_type": "LIST"}


def _filter_orders(orders: list[dict], intent_data: dict) -> list[dict]:
    """
    Backend'den gelen sipariş listesini client-side filtreler.
    Backend status/date filtresi desteklemediğinden Python'da yapıyoruz.
    """
    filtered = orders

    # Status filtresi
    status_filter = intent_data.get("status_filter")
    if status_filter and status_filter != "null":
        filtered = [o for o in filtered if o.get("status", "").upper() == status_filter.upper()]

    # Tarih filtresi (belirli bir gün)
    date_filter = intent_data.get("date_filter")
    if date_filter and date_filter != "null":
        filtered = [o for o in filtered if _match_date(o, date_filter)]

    # Tarih aralığı filtresi
    date_start = intent_data.get("date_range_start")
    date_end = intent_data.get("date_range_end")
    if date_start and date_start != "null":
        filtered = [o for o in filtered if _after_date(o, date_start)]
    if date_end and date_end != "null":
        filtered = [o for o in filtered if _before_date(o, date_end)]

    return filtered


def _match_date(order: dict, date_str: str) -> bool:
    """Siparişin tarihinin verilen tarihle eşleşip eşleşmediğini kontrol eder."""
    created = order.get("createdAt", "")
    if not created:
        return False
    return created[:10] == date_str[:10]


def _after_date(order: dict, date_str: str) -> bool:
    """Siparişin verilen tarihten sonra olup olmadığını kontrol eder."""
    created = order.get("createdAt", "")[:10]
    if not created:
        return False
    return created >= date_str[:10]


def _before_date(order: dict, date_str: str) -> bool:
    """Siparişin verilen tarihten önce olup olmadığını kontrol eder."""
    created = order.get("createdAt", "")[:10]
    if not created:
        return False
    return created <= date_str[:10]


def _format_order_summary(order: dict) -> str:
    """Sipariş özetini okunabilir metne dönüştürür."""
    order_number = order.get("orderNumber", "?")
    status_raw = order.get("status", "PENDING")
    status = ORDER_STATUS_LABELS.get(status_raw, status_raw)
    emoji = STATUS_EMOJIS.get(status_raw, "📋")
    total = order.get("totalAmount", 0)
    created = order.get("createdAt", "")[:10] if order.get("createdAt") else ""
    item_count = len(order.get("items", []))

    lines = [f"**{order_number}**"]
    lines.append(f"Durum: {emoji} {status}")
    lines.append(f"Tutar: {total} TL")
    if item_count:
        lines.append(f"Ürün sayısı: {item_count}")
    if created:
        lines.append(f"Tarih: {created}")
    if status_raw == "SHIPPED":
        lines.append("Kargonuz yolda!")
    elif status_raw == "DELIVERED":
        lines.append("Siparişiniz teslim edildi.")

    return "\n".join(lines)


def _build_filter_description(intent_data: dict) -> str:
    """Uygulanan filtre açıklamasını oluşturur."""
    parts = []
    status_filter = intent_data.get("status_filter")
    if status_filter and status_filter != "null":
        label = ORDER_STATUS_LABELS.get(status_filter, status_filter)
        parts.append(f"durum: {label}")

    date_filter = intent_data.get("date_filter")
    if date_filter and date_filter != "null":
        parts.append(f"tarih: {date_filter}")

    date_start = intent_data.get("date_range_start")
    date_end = intent_data.get("date_range_end")
    if date_start and date_start != "null" and date_end and date_end != "null":
        parts.append(f"tarih aralığı: {date_start} — {date_end}")

    if not parts:
        return ""
    return " (" + ", ".join(parts) + ")"


async def generate_order_response(messages: list, orders: list, query_type: str) -> str:
    """Sipariş verilerini kullanarak doğal dilde yanıt oluşturur."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )

    # Sipariş verilerini özetle
    order_data_str = json.dumps(orders, ensure_ascii=False, indent=2)

    try:
        response = await llm.ainvoke([
            SystemMessage(content=ORDER_RESPONSE_PROMPT.format(order_data=order_data_str)),
            *messages[-5:],  # Son 5 mesaj bağlam için yeterli
        ])
        return response.content
    except Exception as e:
        logger.error("order_response_error", error=str(e))
        # Fallback: Basit özet döndür
        if not orders:
            return "Aradığınız kriterlere uygun sipariş bulunamadı."
        summaries = [_format_order_summary(o) for o in orders[:3]]
        return "İşte siparişleriniz:\n\n" + "\n\n".join(summaries)


async def order_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Order Agent node'u.
    Sipariş sorgulama, filtreleme ve detay getirme.
    """
    messages = state["messages"]
    user_id = state.get("user_id")
    user_role = state.get("user_role", "")

    # Giriş kontrolü
    if not user_id:
        return {
            **state,
            "final_response": "Sipariş bilgilerinizi görüntülemek için giriş yapmanız gerekiyor.",
            "agent_type": "order_agent",
            "action_type": "INFO",
        }

    # Sorgu tipini ve filtreleri tespit et (Tüm geçmiş ile)
    intent_data = await parse_order_intent(messages)
    query_type = intent_data.get("query_type", "LIST")
    order_number = intent_data.get("order_number")

    # Filtre var mı kontrol et
    has_filters = any(
        intent_data.get(k) and intent_data.get(k) != "null"
        for k in ["status_filter", "date_filter", "date_range_start", "date_range_end"]
    )

    logger.info("order_agent", query_type=query_type, user_id=user_id,
                has_filters=has_filters, intent_data=intent_data)

    try:
        # ---- Son sipariş / Durum sorgusu ----
        if query_type in ("LATEST", "STATUS") and not has_filters:
            order = await get_latest_order.ainvoke({"user_id": user_id, "user_role": user_role})

            if not order:
                return {
                    **state,
                    "final_response": "Henüz bir siparişiniz bulunmuyor.",
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            if "message" in order and "henüz" in order["message"].lower():
                return {
                    **state,
                    "final_response": order["message"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            # Doğal dilde yanıt üret
            response_text = await generate_order_response(messages, [order], "LATEST")

            return {
                **state,
                "final_response": response_text,
                "agent_type": "order_agent",
                "action_type": "ORDER_INFO",
                "action_data": {"order": order, "is_latest": True},
            }

        # ---- Tüm siparişler (filtreli veya filtresiz) ----
        elif query_type in ("LIST", "LATEST", "STATUS"):
            # Filtreleme için daha fazla sipariş çek
            fetch_size = 50 if has_filters else 5
            result = await get_user_orders.ainvoke({
                "user_id": user_id,
                "user_role": user_role,
                "page": 0,
                "size": fetch_size,
            })

            if "error" in result:
                return {
                    **state,
                    "final_response": result["error"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            orders = result.get("content", [])
            total = result.get("totalElements", len(orders))

            # Client-side filtreleme uygula
            if has_filters:
                orders = _filter_orders(orders, intent_data)

            if not orders:
                status_desc = _build_filter_description(intent_data)
                return {
                    **state,
                    "final_response": f"Aradığınız kriterlere uygun sipariş bulunamadı{status_desc}.",
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            # Doğal dilde yanıt üret
            response_text = await generate_order_response(messages, orders, "LIST")

            return {
                **state,
                "final_response": response_text,
                "agent_type": "order_agent",
                "action_type": "ORDER_LIST",
                "action_data": {
                    "orders": orders,
                    "filters": intent_data,
                    "total": total
                },
            }

        # ---- Sipariş detayı ----
        elif query_type == "DETAIL" and order_number:
            order = await get_order_detail.ainvoke({
                "user_id": user_id,
                "order_number": order_number,
                "user_role": user_role,
            })

            if "error" in order:
                return {
                    **state,
                    "final_response": order["error"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            # Doğal dilde yanıt üret
            response_text = await generate_order_response(messages, [order], "DETAIL")

            return {
                **state,
                "final_response": response_text,
                "agent_type": "order_agent",
                "action_type": "ORDER_INFO",
                "action_data": {"order": order},
            }

        else:
            return {
                **state,
                "final_response": "Sipariş numaranızı (ör. ORD-20240101-XXXX) paylaşırsanız "
                                  "detaylı bilgi verebilirim.",
                "agent_type": "order_agent",
                "action_type": "INFO",
            }

    except Exception as e:
        logger.error("order_agent_error", error=str(e))
        return {
            **state,
            "final_response": "Sipariş bilgisi alınırken bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "order_agent",
            "error": str(e),
        }
