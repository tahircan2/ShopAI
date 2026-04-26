"""
agents/recommend_agent.py — Ürün öneri agent'ı.

Kullanıcının ilgi alanlarına, görüntülediği ürüne veya genel popülerliğe
göre ürün önerir. Kişiselleştirme için kullanıcı geçmişi kullanılabilir.
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, BaseMessage

from config import settings
from graph.state import AgentState
from tools.product_tools import filter_products, get_featured_products, search_products

logger = structlog.get_logger(__name__)

RECOMMEND_EXTRACT_PROMPT = """Sen bir ürün öneri sistemisin.
Kullanıcı mesajından öneri bağlamını çıkar ve JSON döndür.

JSON formatı:
{
  "reference_product": "referans ürün adı veya kategorisi (varsa)",
  "preference_keywords": ["kullanıcının belirttiği özellikler"],
  "recommendation_type": "SIMILAR|POPULAR|PERSONALIZED|CATEGORY",
  "category": "kategori adı (varsa)"
}

Öneri tipleri:
- SIMILAR: "buna benzer", "bunun gibi", "alternatif"
- POPULAR: "popüler", "çok satan", "beğenilen", "trend"
- PERSONALIZED: "ne önerirsin", "bana uygun", "önerir misin"
- CATEGORY: belirli bir kategori için öneri

SADECE JSON döndür."""


async def extract_recommendation_context(messages: list[BaseMessage]) -> dict:
    """Sohbet geçmişinden öneri bağlamını çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=RECOMMEND_EXTRACT_PROMPT),
            *messages,
        ])
        return json.loads(response.content)
    except Exception as e:
        logger.error("recommend_context_error", error=str(e))
        return {"recommendation_type": "POPULAR"}


RECOMMEND_RESPONSE_PROMPT = """Siz ShopAI'nın kişisel alışveriş danışmanısınız. 
Kullanıcının zevkine veya ihtiyacına göre özenle seçilmiş ürünleri, profesyonel ve ilham verici bir dille sunun.

STRATEJİ:
1. **Kişiselleştirilmiş Giriş**: "Zevkinize hitap edebileceğini düşündüğüm seçkilerim şunlardır:" gibi nazik bir giriş yapın.
2. **Ürün Seçkisi**: En fazla 3 ürünü, neden önerdiğinizi belirten kısa (birer cümlelik) notlarla listeleyin.
3. **Format**: Markdown kullanarak ürün adlarını ve fiyatlarını netleştirin.
4. **Hitap**: Her zaman 'Siz' dilini ve nazik bir tonu koruyun.
5. **Görsellik**: Yıldız (⭐) ve Paket (🎁) gibi emojilerle sunumu zenginleştirin.
6. **DÜRÜSTLÜK (ÇOK ÖNEMLİ)**: KESİNLİKLE hayali, uydurma ürünler oluşturmayın. SADECE size sağlanan kullanılabilir ürünler listesinde var olan ürünlerden bahsedin. Eğer liste boşsa asla hayali ürün isimleri (örn: Converse, Nike) uydurmayın.
"""


async def generate_recommendation_response(messages: list[BaseMessage], products: list) -> str:
    """LLM ile öneri yanıtı oluşturur."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )

    product_summary = "\n".join([
        f"- {p.get('name', 'Ürün')} — {p.get('discountedPrice') or p.get('price', '?')} TL "
        f"(Puan: {p.get('ratingAvg', '-')})"
        for p in products[:5]
    ])

    context = f"Kullanılabilir ürünler:\n{product_summary}"

    try:
        response = await llm.ainvoke([
            SystemMessage(content=RECOMMEND_RESPONSE_PROMPT),
            *messages,
            HumanMessage(content=context),
        ])
        return response.content
    except Exception as e:
        logger.error("recommend_response_error", error=str(e))
        count = len(products[:3])
        return f"Size {count} ürün önerdim. İnceleyebilirsiniz!"


async def recommend_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Recommend Agent node'u.
    Kullanıcı isteğine göre ürün önerir.
    """
    messages = state["messages"]
    user_id = state.get("user_id")

    # Öneri bağlamını çıkar (Tüm geçmiş ile)
    context = await extract_recommendation_context(messages)
    rec_type = context.get("recommendation_type", "POPULAR")

    logger.info("recommend_agent", rec_type=rec_type, user_id=user_id)

    try:
        products_result = {}

        if rec_type == "POPULAR":
            # En çok değerlendirilen ürünler
            products_result = await filter_products.ainvoke({
                "sort_by": "ratingCount",
                "sort_dir": "desc",
                "size": 8,
                "user_id": user_id,
            })

        elif rec_type == "SIMILAR":
            # Referans ürüne benzer — aynı kategori/arama
            ref = context.get("reference_product", "")
            if ref:
                products_result = await search_products.ainvoke({
                    "query": ref,
                    "user_id": user_id,
                    "size": 8,
                })
            else:
                products_result = await get_featured_products.ainvoke({"user_id": user_id})

        elif rec_type == "CATEGORY":
            category = context.get("category", "")
            keywords = context.get("preference_keywords", [])
            products_result = await filter_products.ainvoke({
                "category": category if category else None,
                "q": " ".join(keywords) if keywords else None,
                "sort_by": "rating",
                "sort_dir": "desc",
                "size": 8,
                "user_id": user_id,
            })

        else:
            # PERSONALIZED — öne çıkan + yüksek puanlı
            products_result = await filter_products.ainvoke({
                "sort_by": "rating",
                "sort_dir": "desc",
                "size": 8,
                "user_id": user_id,
            })

        content = products_result.get("content", [])

        if not content:
            # Fallback: öne çıkan ürünler
            featured = await get_featured_products.ainvoke({"user_id": user_id})
            content = featured.get("content", [])

        if not content:
            return {
                **state,
                "final_response": "Şu an öneri yapacak ürün bulunamadı. "
                                  "Ürün listemize göz atmak ister misiniz?",
                "agent_type": "recommend_agent",
                "action_type": "INFO",
            }

        # LLM ile açıklayıcı yanıt oluştur
        response_text = await generate_recommendation_response(messages, content)

        return {
            **state,
            "final_response": response_text,
            "agent_type": "recommend_agent",
            "action_type": "PRODUCT_LIST",
            "action_data": {
                "products": products_result,
                "recommendation_type": rec_type,
            },
        }

    except Exception as e:
        logger.error("recommend_agent_error", error=str(e))
        return {
            **state,
            "final_response": "Öneri oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "recommend_agent",
            "error": str(e),
        }
