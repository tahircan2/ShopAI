"""
agents/order_agent.py — Sipariş durumu sorgulama agent'ı.

Kullanıcının sipariş durumunu, kargo takibini ve sipariş geçmişini sorgular.
Tüm sorgular JWT'den extract edilen user_id ile scope edilir.
Bir kullanıcı başka kullanıcının siparişini sorgulayamaz.
"""

import json
import re
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState
from tools.order_tools import get_user_orders, get_order_detail, get_latest_order

logger = structlog.get_logger(__name__)

ORDER_INTENT_PROMPT = """Sen bir sipariş sorgu sistemisin.
Kullanıcı mesajından sipariş sorgusunu çıkar. JSON döndür.

JSON formatı:
{
  "query_type": "LATEST|LIST|DETAIL|STATUS",
  "order_number": "sipariş numarası (varsa, ör. ORD-20240101-XXXX)"
}

Sorgular:
- LATEST: "son siparişim", "en son siparişim", "son kargom"
- LIST: "siparişlerim", "tüm siparişlerim", "sipariş geçmişim"
- DETAIL: sipariş numarası belirtilmişse
- STATUS: "nerede", "ne zaman gelir", "kargo durumu"

SADECE JSON döndür."""

ORDER_STATUS_LABELS = {
    "PENDING": "Beklemede",
    "CONFIRMED": "Onaylandı",
    "SHIPPED": "Kargoya Verildi",
    "DELIVERED": "Teslim Edildi",
    "CANCELLED": "İptal Edildi",
    "REFUNDED": "İade Edildi",
}


def _extract_order_number(message: str) -> str | None:
    """Mesajdan sipariş numarasını çıkarır."""
    pattern = r"ORD-\d{8}-[A-Z0-9]{4}"
    match = re.search(pattern, message.upper())
    return match.group(0) if match else None


async def parse_order_intent(message: str) -> dict:
    """Kullanıcı mesajından sipariş sorgusunu çıkarır."""
    # Önce regex ile sipariş numarası ara (hızlı)
    order_number = _extract_order_number(message)
    if order_number:
        return {"query_type": "DETAIL", "order_number": order_number}

    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=ORDER_INTENT_PROMPT),
            HumanMessage(content=message),
        ])
        return json.loads(response.content)
    except Exception as e:
        logger.error("order_intent_parse_error", error=str(e))
        return {"query_type": "LATEST"}


def _format_order_summary(order: dict) -> str:
    """Sipariş özetini okunabilir metne dönüştürür."""
    order_number = order.get("orderNumber", "?")
    status_raw = order.get("status", "PENDING")
    status = ORDER_STATUS_LABELS.get(status_raw, status_raw)
    total = order.get("totalAmount", 0)
    created = order.get("createdAt", "")[:10] if order.get("createdAt") else ""
    item_count = len(order.get("items", []))

    lines = [f"**{order_number}**"]
    lines.append(f"Durum: {status}")
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


async def order_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Order Agent node'u.

    KRİTİK: user_id state'ten alınır (Spring Boot JWT'den).
    Kullanıcı mesajından asla alınmaz.
    Spring Boot ownership check uygular — cross-user erişim engellenir.
    """
    message = state["current_message"]
    user_id = state.get("user_id")

    # Giriş kontrolü
    if not user_id:
        return {
            **state,
            "final_response": "Sipariş bilgilerinizi görüntülemek için giriş yapmanız gerekiyor.",
            "agent_type": "order_agent",
            "action_type": "INFO",
        }

    # Sorgu tipini tespit et
    intent_data = await parse_order_intent(message)
    query_type = intent_data.get("query_type", "LATEST")
    order_number = intent_data.get("order_number")

    logger.info("order_agent", query_type=query_type, user_id=user_id)

    try:
        # ---- Son sipariş / Durum sorgusu ----
        if query_type in ("LATEST", "STATUS"):
            order = await get_latest_order.ainvoke({"user_id": user_id})

            if "message" in order and "henüz" in order["message"].lower():
                return {
                    **state,
                    "final_response": "Henüz hiç sipariş vermediniz. "
                                      "Ürünlere göz atarak ilk siparişinizi oluşturabilirsiniz!",
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            if "error" in order:
                return {
                    **state,
                    "final_response": order["error"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            summary = _format_order_summary(order)
            return {
                **state,
                "final_response": f"En son siparişiniz:\n\n{summary}",
                "agent_type": "order_agent",
                "action_type": "ORDER_INFO",
                "action_data": {"order": order},
            }

        # ---- Tüm siparişler ----
        elif query_type == "LIST":
            result = await get_user_orders.ainvoke({
                "user_id": user_id,
                "page": 0,
                "size": 5,
            })

            if "error" in result:
                return {
                    **state,
                    "final_response": result["error"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            orders = result.get("content", [])
            total = result.get("totalElements", 0)

            if not orders:
                return {
                    **state,
                    "final_response": "Henüz hiç siparişiniz bulunmuyor.",
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            summaries = [_format_order_summary(o) for o in orders[:3]]
            response_text = f"Toplam {total} siparişiniz var. Son {len(summaries)} tanesi:\n\n"
            response_text += "\n\n---\n".join(summaries)

            return {
                **state,
                "final_response": response_text,
                "agent_type": "order_agent",
                "action_type": "ORDER_INFO",
                "action_data": {"orders": result},
            }

        # ---- Sipariş detayı ----
        elif query_type == "DETAIL" and order_number:
            order = await get_order_detail.ainvoke({
                "user_id": user_id,
                "order_number": order_number,
            })

            if "error" in order:
                return {
                    **state,
                    "final_response": order["error"],
                    "agent_type": "order_agent",
                    "action_type": "INFO",
                }

            summary = _format_order_summary(order)
            return {
                **state,
                "final_response": f"Sipariş detayı:\n\n{summary}",
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
