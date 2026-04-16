"""
agents/filter_agent.py — Doğal dil → ürün filtresi dönüşümü ve Doğal Yanıt Üretimi.

Kullanıcının doğal dil ürün taleplerini Spring Boot query parametrelerine çevirir.
Sonuçları aldıktan sonra, LLM kullanarak profesyonel, asistan vari bir yanıt metni üretir.
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
Sana kullanıcının son mesajı ve varsa önceki sohbet geçmişi verilecek.
Kullanıcı mesajından ürün filtresi parametrelerini çıkar ve JSON formatında döndür.

ÖNEMLİ KURALLAR:
1. Eğer kullanıcının son mesajı önceki aramayla tamamen farklı bir niyet taşıyorsa (örn: önce tişört arayıp sonra sweatshirt diyorsa), önceki filtreleri DAHİL ETME, SADECE YENİ MESAJI baz al.
2. Sadece ve sadece bahsi geçen özellikleri JSON olarak çıkar. Olmayan bir şeyi (ör: tişört, ayakkabı) kategori olarak tahmin etme veya örneklerden esinlenme.
3. Fiyatlar TL cinsindendir.

Çıkarabileceğin alanlar:
- category: string (kullanıcı açıkça belirtmişse kategori adı yaz)
- min_price: number (TL)
- max_price: number (TL)
- colors: array of strings 
- sizes: array of strings 
- brand: string 
- rating: number
- sort_by: string (price | rating | ratingCount | createdAt)
- sort_dir: string (asc | desc)
- q: string (kategori harici tam metin arama anahtar kelimeleri)

SADECE geçerli JSON döndür, açıklama veya markdown ekleme.
"""

RESPONSE_SYSTEM_PROMPT = """Sen ShopAI adında kibar, yardımsever ve profesyonel bir e-ticaret asistanısın.
Görev: Sistemden dönen arama/filtreleme sonuçlarını kullanıcıya açıklayıcı, doğal ve ÇOK DÜZENLİ bir dille sunmak.

KURALLAR:
1. Kullanıcıya robotik ("x ürün bulundu") cümleler kurma. "Aramanıza uygun şu ürünleri buldum:" gibi bir giriş yap.
2. Eğer ürün bulunamazsa, filtreleri esnetmesini veya başka kelimelerle aramasını samimi bir şekilde tavsiye et.
3. Listelenen ilk birkaç ürünü ÖZELLİKLE Numaralandırılmış Liste (1., 2., 3. vb.) şeklinde göstererek, her bir ürünün adını ve fiyatını alt alta temiz bir formatta yaz. Kullanıcının gözünü yorma.
4. Yazımda sadece temiz metin ve satır atlamaları kullan (Markdown kullanabilirsin ancak satır aralıklarına dikkat et, çok sıkışık olmasın).
"""

async def extract_filter_params(messages: list) -> dict:
    """
    Kullanıcı mesaj geçmişinden filtre parametrelerini çıkarır.
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
        response_format={"type": "json_object"},
    )

    try:
        messages_with_sys = [SystemMessage(content=FILTER_SYSTEM_PROMPT)] + messages
        response = await llm.ainvoke(messages_with_sys)
        params = json.loads(response.content)
        logger.info("filter_params_extracted", params=params)
        return params
    except json.JSONDecodeError as e:
        logger.error("filter_params_json_error", error=str(e))
        return {}
    except Exception as e:
        logger.error("filter_params_extraction_error", error=str(e))
        return {}


async def generate_conversational_response(messages: list, filter_params: dict, products_result: dict) -> str:
    """
    Arama sonuçlarını yorumlayıp doğal insansı bir yanıt oluşturur.
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )
    
    total = products_result.get("totalElements", 0)
    items = products_result.get("content", [])[:3]  # Örnek ilk 3 ürün
    
    context_msg = f"""
Sistem Arama Sonuç Özeti:
- Uygulanan Filtreler (JSON): {json.dumps(filter_params, ensure_ascii=False)}
- Bulunan Toplam Ürün Sayısı: {total}
- Örnek Listelenen İlk Ürünler: {[{'isim': i.get('name'), 'fiyat': i.get('price')} for i in items]}

Yönerge: Yukarıdaki sonuç verilerini göz önüne alarak, kullanıcının son mesajına istinaden yönlendirici ve doğal bir Türkçe yanıt üret. Kısa ve öz ol (Maksimum 2-3 paragraf).
"""
    try:
        # Son kullanıcı mesajını ve sistem sonucunu LLM'e ver
        messages_with_sys = [
            SystemMessage(content=RESPONSE_SYSTEM_PROMPT),
            messages[-1],  # kullanıcının arama cümlesi
            HumanMessage(content=context_msg)
        ]
        response = await llm.ainvoke(messages_with_sys)
        return response.content
    except Exception as e:
        logger.error("conversational_response_error", error=str(e))
        return f"{total} ürün bulundu."


async def filter_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Filter Agent node'u.
    Mesaj geçmişinden filtre çıkarır, Spring Boot'a sorgu yapar, doğal dilde yanıt oluşturur ve state'e yazar.
    """
    messages = state["messages"]
    user_id = state.get("user_id")

    # 1. Filtre parametrelerini çıkar (Context'i geçmişten al)
    params = await extract_filter_params(messages)

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

        # 3. Hata veya Boş Sonuç Kontrolü
        content = result.get("content", [])
        total = result.get("totalElements", 0)

        if "error" in result:
            return {
                **state,
                "final_response": f"Ürünler aranırken geçici bir teknik sorun oluştu: {result['error']}",
                "agent_type": "filter_agent",
                "action_type": "INFO",
            }

        # 4. Profesyonel Conversational Yanıt Üret
        response_text = await generate_conversational_response(messages, params, result)

        logger.info("filter_agent_success", total=total, params=params, user_id=user_id)

        return {
            **state,
            "final_response": response_text,
            "agent_type": "filter_agent",
            "action_type": "PRODUCT_LIST" if content else "INFO",
            "action_data": {
                "filters": params,
                "products": result,
            },
        }

    except Exception as e:
        logger.error("filter_agent_error", error=str(e))
        return {
            **state,
            "final_response": "Ürün listesi alınırken bir hata oluştu. Lütfen birazdan tekrar deneyiniz.",
            "agent_type": "filter_agent",
            "error": str(e),
        }
