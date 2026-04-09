"""
agents/filter_agent.py — Doğal dil → ürün filtresi dönüşümü.

Kullanıcının doğal dil ürün taleplerini Spring Boot query parametrelerine çevirir.
Örnek: "200 TL altı kırmızı nike ayakkabı" → {maxPrice: 200, colors: ['Kırmızı'], brand: 'Nike', category: 'Ayakkabı'}
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState
from tools.product_tools import filter_products, search_products

logger = structlog.get_logger(__name__)

FILTER_SYSTEM_PROMPT = """Sen bir e-ticaret ürün filtresi oluşturma sistemisin.
Kullanıcı mesajından ürün filtresi parametrelerini çıkar ve JSON formatında döndür.

Çıkarabileceğin parametreler:
- category: string (ör. "Ayakkabı", "Tişört", "Laptop")
- min_price: number (TL cinsinden minimum fiyat)
- max_price: number (TL cinsinden maksimum fiyat)
- colors: array of strings (ör. ["Kırmızı", "Mavi"])
- sizes: array of strings (ör. ["S", "M", "L", "XL", "42", "43"])
- brand: string (marka adı, ör. "Nike", "Adidas", "Apple")
- rating: number (minimum puan, 1-5 arası)
- sort_by: string (price | rating | ratingCount | createdAt)
- sort_dir: string (asc | desc)
- q: string (tam metin arama için anahtar kelimeler)

Kurallar:
- Mesajda belirtilmeyen parametreleri ekleme
- Fiyat Türk Lirası cinsindendir
- Büyük/küçük harf tutarlı kullan (Türkçe için: Kırmızı, Mavi, Yeşil)
- "en ucuz" → sort_by: "price", sort_dir: "asc"
- "en pahalı" → sort_by: "price", sort_dir: "desc"  
- "en çok değerlendirilen" → sort_by: "ratingCount", sort_dir: "desc"
- "en çok beğenilen" veya "en iyi" → sort_by: "rating", sort_dir: "desc"
- "yeni" veya "son eklenen" → sort_by: "createdAt", sort_dir: "desc"

SADECE geçerli JSON döndür. Açıklama ekleme. Markdown kullanma.
Örnek çıktı: {"category": "Ayakkabı", "max_price": 200, "colors": ["Kırmızı"], "brand": "Nike"}"""


async def extract_filter_params(message: str) -> dict:
    """
    Kullanıcı mesajından filtre parametrelerini çıkarır.

    KRİTİK: message ASLA sistem promptuna concat edilmez.
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=FILTER_SYSTEM_PROMPT),
            HumanMessage(content=message),  # Kullanıcı girdisi her zaman ayrı mesaj
        ])

        params = json.loads(response.content)
        logger.info("filter_params_extracted", params=params)
        return params
    except json.JSONDecodeError as e:
        logger.error("filter_params_json_error", error=str(e))
        return {}
    except Exception as e:
        logger.error("filter_params_extraction_error", error=str(e))
        return {}


async def filter_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Filter Agent node'u.
    Mesajdan filtre çıkarır, Spring Boot'a sorgu yapar, sonucu state'e yazar.
    """
    message = state["current_message"]
    user_id = state.get("user_id")

    # 1. Filtre parametrelerini çıkar
    params = await extract_filter_params(message)

    try:
        # 2. Spring Boot'a filtre sorgusu gönder
        if params.get("q") and not any(k in params for k in ["category", "colors", "sizes", "brand"]):
            # Sadece arama terimi varsa full-text search kullan
            result = await search_products.ainvoke({
                "query": params["q"],
                "user_id": user_id,
                "page": params.get("page", 0),
                "size": params.get("size", 10),
            })
        else:
            # Yapılandırılmış filtre
            result = await filter_products.ainvoke({
                **params,
                "user_id": user_id,
            })

        # 3. Sonucu değerlendir
        content = result.get("content", [])
        total = result.get("totalElements", 0)

        if "error" in result:
            return {
                **state,
                "final_response": f"Ürünler listelenirken bir sorun oluştu: {result['error']}",
                "agent_type": "filter_agent",
                "action_type": "INFO",
            }

        if not content:
            return {
                **state,
                "final_response": "Arama kriterlerinize uygun ürün bulunamadı. "
                                  "Filtreleri değiştirerek tekrar deneyebilirsiniz.",
                "agent_type": "filter_agent",
                "action_type": "INFO",
                "action_data": {"filters": params, "totalElements": 0},
            }

        # 4. Kullanıcıya yanıt oluştur
        response_text = f"{total} ürün bulundu."
        if params.get("max_price"):
            response_text += f" (Max {params['max_price']} TL)"
        if params.get("brand"):
            response_text += f" {params['brand']} markası filtrelendi."

        logger.info("filter_agent_success", total=total, params=params, user_id=user_id)

        return {
            **state,
            "final_response": response_text,
            "agent_type": "filter_agent",
            "action_type": "PRODUCT_LIST",
            "action_data": {
                "filters": params,
                "products": result,
            },
        }

    except Exception as e:
        logger.error("filter_agent_error", error=str(e))
        return {
            **state,
            "final_response": "Ürün listesi alınırken bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "filter_agent",
            "error": str(e),
        }
