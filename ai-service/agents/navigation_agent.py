"""
agents/navigation_agent.py — Kullanıcıyı sitenin farklı bölümlerine yönlendiren agent.

Basit ve güvenilir mimari:
  1. LLM, kullanıcının gitmek istediği yeri tespit eder (JSON)
  2. İlgili tool doğrudan çağrılır
  3. State'e NAVIGATE action_type yazılır → Frontend yönlendirme yapar
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, BaseMessage

from config import settings
from graph.state import AgentState
from tools.product_tools import search_products, get_categories

logger = structlog.get_logger(__name__)

NAVIGATION_SYSTEM_PROMPT = """Sen bir navigasyon asistanısın.
Kullanıcı mesajını analiz et ve gitmek istediği yeri tespit et. JSON döndür.

JSON formatı:
{
  "target_type": "PAGE|PRODUCT|CATEGORY",
  "page_name": "HOME|SHOP|CART|CHECKOUT|PROFILE|ORDERS|CONTACT",
  "product_query": "ürün arama terimi (target_type=PRODUCT ise)",
  "category_name": "kategori adı (target_type=CATEGORY ise)"
}

Hedefler:
- PAGE: Sabit sayfalara gitme
  "anasayfa/ana sayfa" → HOME
  "ürünler/mağaza/shop/alışveriş" → SHOP
  "sepet/sepetim" → CART
  "ödeme/checkout" → CHECKOUT
  "profil/profilim/hesabım" → PROFILE
  "siparişlerim/sipariş geçmişim" → ORDERS
  "iletişim/destek" → CONTACT

- PRODUCT: Belirli bir ürüne gitme
  "Nike ayakkabı sayfasına git" → product_query = "Nike ayakkabı"

- CATEGORY: Kategoriye gitme
  "elektronik ürünlere bak" → category_name = "elektronik"

SADECE JSON döndür."""

# Sayfa → URL mapping
PAGE_ROUTES = {
    "HOME": "/",
    "SHOP": "/shop",
    "CART": "/cart",
    "CHECKOUT": "/checkout",
    "PROFILE": "/profile",
    "ORDERS": "/profile/orders",
    "CONTACT": "/contact",
}

# Sayfa → Türkçe isim
PAGE_LABELS = {
    "HOME": "Ana Sayfa",
    "SHOP": "Mağaza",
    "CART": "Sepetim",
    "CHECKOUT": "Ödeme",
    "PROFILE": "Profilim",
    "ORDERS": "Siparişlerim",
    "CONTACT": "İletişim",
}


async def parse_navigation_intent(messages: list[BaseMessage]) -> dict:
    """Sohbet geçmişinden navigasyon hedefini çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=NAVIGATION_SYSTEM_PROMPT),
            *messages,
        ])
        return json.loads(response.content)
    except Exception as e:
        logger.error("navigation_intent_parse_error", error=str(e))
        return {"target_type": "PAGE", "page_name": "HOME"}


async def navigation_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Navigation Agent node'u.
    Kullanıcıyı doğru sayfaya yönlendirir.
    """
    messages = state["messages"]
    user_id = state.get("user_id")

    logger.info("navigation_agent_start", user_id=user_id)

    intent = await parse_navigation_intent(messages)
    target_type = intent.get("target_type", "PAGE")

    # ---- PAGE: Sabit sayfaya yönlendirme ----
    if target_type == "PAGE":
        page_name = intent.get("page_name", "HOME").upper()
        path = PAGE_ROUTES.get(page_name, "/")
        label = PAGE_LABELS.get(page_name, page_name)

        return {
            **state,
            "final_response": f"Sizi {label} sayfasına yönlendiriyorum.",
            "agent_type": "navigation_agent",
            "action_type": "NAVIGATE",
            "action_data": {
                "action": "NAVIGATE",
                "path": path,
                "page_name": page_name,
                "params": None,
            },
        }

    # ---- PRODUCT: Ürün detay sayfasına yönlendirme ----
    elif target_type == "PRODUCT":
        product_query = intent.get("product_query", "")
        if not product_query:
            return {
                **state,
                "final_response": "Hangi ürünün sayfasına gitmek istediğinizi belirtir misiniz?",
                "agent_type": "navigation_agent",
                "action_type": "INFO",
            }

        # Ürünü ara
        result = await search_products.ainvoke({
            "query": product_query,
            "user_id": user_id,
            "page": 0,
            "size": 1,
        })

        products = result.get("content", [])
        if not products:
            return {
                **state,
                "final_response": f"'{product_query}' ürünü bulunamadı. Farklı bir arama deneyin.",
                "agent_type": "navigation_agent",
                "action_type": "INFO",
            }

        product = products[0]
        product_id = product.get("id")
        product_slug = product.get("slug", product_id)
        product_name = product.get("name", "Ürün")
        path = f"/product/{product_slug or product_id}"

        return {
            **state,
            "final_response": f"Sizi '{product_name}' ürün sayfasına yönlendiriyorum.",
            "agent_type": "navigation_agent",
            "action_type": "NAVIGATE",
            "action_data": {
                "action": "NAVIGATE",
                "path": path,
                "product_id": product_id,
            },
        }

    # ---- CATEGORY: Kategori sayfasına yönlendirme ----
    elif target_type == "CATEGORY":
        category_name = intent.get("category_name", "")
        if not category_name:
            return {
                **state,
                "final_response": "Hangi kategoriye gitmek istediğinizi belirtir misiniz?",
                "agent_type": "navigation_agent",
                "action_type": "INFO",
            }

        # Slug oluştur
        slug = category_name.lower()
        for old, new in [("ş", "s"), ("ı", "i"), ("ğ", "g"), ("ü", "u"), ("ö", "o"), ("ç", "c"), (" ", "-")]:
            slug = slug.replace(old, new)

        return {
            **state,
            "final_response": f"Sizi '{category_name}' kategorisine yönlendiriyorum.",
            "agent_type": "navigation_agent",
            "action_type": "NAVIGATE",
            "action_data": {
                "action": "NAVIGATE",
                "path": "/shop",
                "params": {"category": slug},
            },
        }

    # Fallback
    return {
        **state,
        "final_response": "Nereye gitmek istediğinizi anlamadım. Ana sayfaya yönlendiriyorum.",
        "agent_type": "navigation_agent",
        "action_type": "NAVIGATE",
        "action_data": {
            "action": "NAVIGATE",
            "path": "/",
            "page_name": "HOME",
        },
    }
