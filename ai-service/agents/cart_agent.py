"""
agents/cart_agent.py — Sepet işlemleri agent'ı.

AI üzerinden sepete ekleme, çıkarma ve görüntüleme işlemleri yapar.
KRİTİK: Tüm sepet işlemleri user_id'yi Spring Boot API header'ından geçirir.
user_id ASLA kullanıcı mesajından alınmaz.
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, BaseMessage

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

ÖNEMLİ: Eğer sohbet geçmişinde daha önce gösterilen bir ürünün adı geçiyorsa (ör. kullanıcı önce
ürün araması yaptı, sonra "onu sepetime ekle" dedi), geçmişteki ürün adını product_query'ye yaz.

SADECE JSON döndür."""


async def parse_cart_intent(messages: list[BaseMessage]) -> dict:
    """Sohbet geçmişinden sepet aksiyonunu çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=CART_INTENT_SYSTEM_PROMPT),
            *messages,
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
    """
    messages = state["messages"]
    user_id = state.get("user_id")

    # Giriş yapmamış kullanıcı kontrolü — hem None hem boş string kontrol et
    if not user_id or str(user_id).strip() == "":
        logger.warning("cart_agent_no_user_id", user_id_raw=user_id,
                       session_id=state.get("session_id"))
        return {
            **state,
            "final_response": "Sepet işlemlerinizi gerçekleştirebilmemiz için lütfen önce hesabınıza giriş yapınız. "
                               "Sağ üst köşedeki giriş panelini kullanabilirsiniz.",
            "agent_type": "cart_agent",
            "action_type": "INFO",
        }

    logger.info("cart_agent_start", user_id=user_id, session_id=state.get("session_id"))

    # Aksiyonu tespit et (Tüm geçmiş ile)
    intent_data = await parse_cart_intent(messages)
    action = intent_data.get("action", "GET").upper()

    logger.info("cart_action", action=action, user_id=user_id, intent_data=intent_data)

    # ---- GET ----
    if action == "GET":
        cart = await get_cart.ainvoke({"user_id": user_id})

        if "error" in cart:
            logger.error("cart_get_error", error=cart["error"], user_id=user_id)
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
                "final_response": "Sepetiniz şu an boş görünüyor. Sizin için seçtiğimiz özel ürünlere göz atmak ister misiniz?",
                "agent_type": "cart_agent",
                "action_type": "CART_UPDATED",
                "action_data": cart,
            }

        total = cart.get("total", 0)
        count = len(items)
        return {
            **state,
            "final_response": f"Sepetinizde güncel olarak **{count}** adet ürün bulunmaktadır. Toplam tutarınız: **{total} TL**.",
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
                "final_response": "Lütfen sepetinize eklemek istediğiniz ürünü belirtiniz. "
                                  "Örneğin: 'Nike spor ayakkabıyı sepetime ekle'",
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
                "size": 5,
            })
            products = result.get("content", [])
            # En iyi eşleşmeyi bul (ilk sonuç)
            product = products[0] if products else None

        if not product:
            return {
                **state,
                "final_response": f"Üzgünüm, '{product_query}' araması için uygun bir ürün bulamadım. "
                                  "Lütfen farklı bir anahtar kelime ile tekrar deneyiniz.",
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
            logger.error("cart_add_error", error=cart["error"], user_id=user_id,
                         product_id=product.get("id"))
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
            "final_response": f"✨ **{product_name}** ({price} TL) başarıyla sepetinize eklendi. Alışverişinize keyifle devam edebilirsiniz!",
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
            "final_response": "Sepetiniz tamamen temizlenmiştir. Yeni seçimleriniz için hazırız! ✨",
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
