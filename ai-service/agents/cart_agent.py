"""
agents/cart_agent.py — Sepet işlemleri agent'ı.

AI üzerinden sepete ekleme, çıkarma ve görüntüleme işlemleri yapar.
KRİTİK: Tüm sepet işlemleri user_id'yi Spring Boot API header'ından geçirir.
user_id ASLA kullanıcı mesajından alınmaz.
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState
from tools.cart_tools import get_cart, add_to_cart, clear_cart, remove_from_cart
from tools.product_tools import filter_products, search_products

logger = structlog.get_logger(__name__)

CART_INTENT_SYSTEM_PROMPT = """Sen bir sepet işlemleri sınıflandırma sistemisin.
Kullanıcı mesajından sepet aksiyonunu ve parametrelerini çıkar. JSON döndür.

Aksiyonlar:
- GET: Sepeti görüntüle ("sepetimde ne var", "sepetimi göster")
- ADD: Ürün ekle ("sepetime ekle", "en ucuzunu sepetime at", "bunu al")
- REMOVE: Ürün çıkar ("sepetten çıkar", "kaldır")
- CLEAR: Sepeti temizle ("sepetimi temizle", "hepsini sil")

JSON formatı:
{
  "action": "GET|ADD|REMOVE|CLEAR",
  "product_query": "arama terimi (ADD için)",
  "quantity": 1,
  "find_cheapest": true/false (ADD için "en ucuz" denmişse true)
}

SADECE JSON döndür."""


async def parse_cart_intent(message: str) -> dict:
    """Kullanıcı mesajından sepet aksiyonunu çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=CART_INTENT_SYSTEM_PROMPT),
            HumanMessage(content=message),
        ])
        return json.loads(response.content)
    except Exception as e:
        logger.error("cart_intent_parse_error", error=str(e))
        return {"action": "GET"}


async def find_cheapest_product(query: str, user_id: str | None) -> dict | None:
    """Sorgu ile en ucuz ürünü bulur."""
    result = await search_products.ainvoke({
        "query": query,
        "user_id": user_id,
        "page": 0,
        "size": 20,
    })

    content = result.get("content", [])
    if not content:
        # Fallback: filter ile dene
        result = await filter_products.ainvoke({
            "q": query,
            "sort_by": "price",
            "sort_dir": "asc",
            "size": 1,
            "user_id": user_id,
        })
        content = result.get("content", [])

    if not content:
        return None

    # Fiyata göre sırala, en ucuzu al
    sorted_products = sorted(
        content,
        key=lambda p: p.get("discountedPrice") or p.get("price", 0),
    )
    return sorted_products[0] if sorted_products else None


async def cart_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Cart Agent node'u.

    KRİTİK GÜVENLİK: user_id state'ten alınır (Spring Boot JWT extract etti).
    Kullanıcı mesajından asla alınmaz.
    """
    message = state["current_message"]
    user_id = state.get("user_id")  # JWT'den — kullanıcı girdisinden değil

    # Giriş yapmamış kullanıcı kontrolü
    if not user_id:
        return {
            **state,
            "final_response": "Sepet işlemleri için giriş yapmanız gerekiyor. "
                              "Sağ üstten giriş yapabilirsiniz.",
            "agent_type": "cart_agent",
            "action_type": "INFO",
        }

    # Aksiyonu tespit et
    intent_data = await parse_cart_intent(message)
    action = intent_data.get("action", "GET").upper()

    logger.info("cart_action", action=action, user_id=user_id)

    # ---- GET ----
    if action == "GET":
        cart = await get_cart.ainvoke({"user_id": user_id})

        if "error" in cart:
            return {
                **state,
                "final_response": cart["error"],
                "agent_type": "cart_agent",
                "action_type": "INFO",
            }

        items = cart.get("items", [])
        if not items:
            return {
                **state,
                "final_response": "Sepetiniz şu an boş. Ürünlere göz atmak ister misiniz?",
                "agent_type": "cart_agent",
                "action_type": "CART_UPDATED",
                "action_data": cart,
            }

        total = cart.get("total", 0)
        count = len(items)
        return {
            **state,
            "final_response": f"Sepetinizde {count} ürün var. Toplam tutar: {total} TL.",
            "agent_type": "cart_agent",
            "action_type": "CART_UPDATED",
            "action_data": cart,
        }

    # ---- ADD ----
    elif action == "ADD":
        product_query = intent_data.get("product_query", "")
        quantity = intent_data.get("quantity", 1)
        find_cheapest = intent_data.get("find_cheapest", False)

        if not product_query:
            return {
                **state,
                "final_response": "Hangi ürünü eklemek istediğinizi belirtin. "
                                  "Örnek: 'Nike ayakkabıyı sepetime ekle'",
                "agent_type": "cart_agent",
                "action_type": "INFO",
            }

        # Ürünü bul
        if find_cheapest:
            product = await find_cheapest_product(product_query, user_id)
        else:
            result = await search_products.ainvoke({
                "query": product_query,
                "user_id": user_id,
                "page": 0,
                "size": 1,
            })
            products = result.get("content", [])
            product = products[0] if products else None

        if not product:
            return {
                **state,
                "final_response": f"'{product_query}' araması için ürün bulunamadı. "
                                  "Farklı bir arama terimi deneyin.",
                "agent_type": "cart_agent",
                "action_type": "INFO",
            }

        # Sepete ekle
        cart = await add_to_cart.ainvoke({
            "user_id": user_id,
            "product_id": product["id"],
            "quantity": quantity,
        })

        if "error" in cart:
            return {
                **state,
                "final_response": cart["error"],
                "agent_type": "cart_agent",
                "action_type": "INFO",
            }

        product_name = product.get("name", "Ürün")
        price = product.get("discountedPrice") or product.get("price", 0)
        return {
            **state,
            "final_response": f"✓ '{product_name}' ({price} TL) sepetinize eklendi!",
            "agent_type": "cart_agent",
            "action_type": "CART_UPDATED",
            "action_data": cart,
        }

    # ---- CLEAR ----
    elif action == "CLEAR":
        result = await clear_cart.ainvoke({"user_id": user_id})

        if "error" in result:
            return {
                **state,
                "final_response": result["error"],
                "agent_type": "cart_agent",
                "action_type": "INFO",
            }

        return {
            **state,
            "final_response": "Sepetiniz temizlendi.",
            "agent_type": "cart_agent",
            "action_type": "CART_UPDATED",
            "action_data": {"items": [], "total": 0},
        }

    # Bilinmeyen aksiyon
    else:
        return {
            **state,
            "final_response": "Sepet işleminizi anlayamadım. "
                              "Örnek: 'Sepetimi göster', 'Nike ayakkabı ekle', 'Sepeti temizle'",
            "agent_type": "cart_agent",
            "action_type": "INFO",
        }
