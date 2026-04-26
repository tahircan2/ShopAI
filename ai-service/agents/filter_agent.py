"""
agents/filter_agent.py — Doğal dil → ürün filtresi dönüşümü ve Doğal Yanıt Üretimi.

Kullanıcının doğal dil ürün taleplerini Spring Boot query parametrelerine çevirir.
Sonuçları aldıktan sonra, LLM kullanarak profesyonel, asistan vari bir yanıt metni üretir.

DETAY MODU:
  Intent PRODUCT_DETAIL ise, ürünü arar, bulursa get_product_detail ile
  detaylı bilgiyi getirir ve doğal dilde sunar.
"""

import json
import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState
from tools.product_tools import filter_products, search_products, get_product_detail

logger = structlog.get_logger(__name__)

FILTER_SYSTEM_PROMPT = """Sen bir e-ticaret ürün filtresi oluşturma sistemisin.
Sana kullanıcının son mesajı ve varsa önceki sohbet geçmişi verilecek.
Kullanıcı mesajından ürün filtresi parametrelerini çıkar ve JSON formatında döndür.

ÖNEMLİ KURALLAR:
1. Eğer kullanıcının son mesajı önceki aramayla tamamen farklı bir niyet taşıyorsa (örn: önce tişört arayıp sonra sweatshirt diyorsa), önceki filtreleri DAHİL ETME, SADECE YENİ MESAJI baz al.
2. Ürün adlarını, isimlerini veya genel arama terimlerini (ör: sehpa, ayakkabı, telefon) KESİNLİKLE 'q' parametresi (serbest metin arama) olarak çıkar. Sadece kullanıcı açıkça "kategori: mobilya" gibi bir ifade kullanırsa category yap.
3. ÇOK ÖNEMLİ: 'q' parametresi arama motoruna gideceği için onu YALINLAŞTIR: 
   - Çoğul eklerini kaldırarak tekil form kullan ("ayakkabılar" veya "ayakkabıları" yerine "ayakkabı", "tişörtler" yerine "tişört"). 
   - Renk veya beden gibi sıfatları 'q' içinden çıkarıp ilgili alanlara (colors, sizes) koy ("kırmızı spor ayakkabı" -> q="spor ayakkabı", colors=["kırmızı"]).
4. Fiyatlar TL cinsindendir.
5. Eğer kullanıcı belirli bir ürünün detayını istiyorsa (ör: "ahşap sehpa hakkında detay ver"), "detail_mode" alanını true yap.

Çıkarabileceğin alanlar:
- q: string (ürün adı, marka veya genel arama kelimeleri - EN ÇOK BUNU KULLAN)
- category: string (SADECE kullanıcı açıkça kategori adı belirtmişse)
- min_price: number (TL)
- max_price: number (TL)
- colors: array of strings 
- sizes: array of strings 
- brand: string 
- rating: number
- sort_by: string (price | rating | ratingCount | createdAt)
  - Örn: "en çok puan alan" veya "en yüksek puan" -> rating
  - Örn: "en çok yorum alan" veya "en çok değerlendirilen" -> ratingCount
  - Örn: "en yüksek fiyatlı" veya "en pahalı" -> price
  - Örn: "en yeni" -> createdAt
- sort_dir: string (asc | desc). (En yüksek/en çok/en yeni için 'desc', En düşük/en ucuz için 'asc')
- size: number (kullanıcı 'en pahalı ürün' gibi tekil bir şey istiyorsa 1, 'ürünler' gibi çoğul ise veya belirtilmemişse 10)
- detail_mode: boolean (kullanıcı ürün detayı istiyorsa true)

SADECE geçerli JSON döndür, açıklama veya markdown ekleme.
"""

RESPONSE_SYSTEM_PROMPT = """Siz ShopAI'nın zarif ve profesyonel e-ticaret danışmanısınız. 
Göreviniz: Sistemden gelen arama/filtreleme sonuçlarını üst düzey bir alışveriş deneyimi sunacak şekilde yapılandırılmış, nazik ve çözüm odaklı bir dille sunmaktır.

UYGULANACAK STANDARTLAR:
1. **Kurumsal Dil**: Her zaman 'Siz' hitabını kullanın. "X ürün bulundu" gibi teknik ifadeler yerine "Aramanızla eşleşen en seçkin seçenekleri sizin için listeledim" gibi daha şık girişler yapın.
2. **Görsel Düzen**: Bilgileri Markdown kullanarak organize edin. Önemli kısımları (marka, fiyat) kalınlaştırın.
3. **Ürün Sunumu**: İlk 3 ürünü bir liste halinde, her birine dair kısa ve çekici bir notla sunun.
4. **Samimiyet ve Rehberlik**: Eğer sonuç azsa veya yoksa, kullanıcıyı hayal kırıklığına uğratmadan alternatif önerilerde bulunun (örn: "Fiyat aralığını biraz genişletmek isterseniz...")
5. **Modern Emoji Kullanımı**: Sektöre uygun emojileri (🛍️, ✨, 🔍) ölçülü şekilde kullanarak metni canlandırın.
6. **DÜRÜSTLÜK (ÇOK ÖNEMLİ)**: KESİNLİKLE hayali, uydurma ürünler oluşturmayın. SADECE size sağlanan JSON formatındaki gerçek arama sonuçlarında var olan ürünlerden bahsedin. Eğer size iletilen JSON sonuçlarında ürün yoksa "Şu Adidas ayakkabıyı alabilirsiniz" gibi hayali öneriler yapmayın!
"""

DETAIL_RESPONSE_PROMPT = """Siz ShopAI'nın uzman ürün danışmanısınız. 
Sana bir ürünün tüm teknik ve ticari detayları verilecek. Bu ürünü kullanıcıya adeta bir mağaza danışmanı samimiyeti ve uzmanlığıyla tanıtın.

SUNUM PLANI:
1. **Giriş**: Ürünün öne çıkan en güçlü yanını belirten şık bir başlık.
2. **Ürün Kimliği**: Fiyat, Marka ve Kategori bilgilerini temiz bir liste halinde sunun.
3. **Detaylı İnceleme**: Ürün açıklamasını, renk ve beden seçeneklerini kullanıcıyı ikna edecek şekilde özetleyin.
4. **Güven Faktörü**: Puan ve yorum sayısını vurgulayarak ürünün popülerliğini belirtin.
5. **Eylem Çağrısı (CTA)**: "Bu şık ürünü sepetinize ekleyerek alışverişinize devam edebilirsiniz" gibi nazik yönlendirmeler yapın.

Önemli: Markdown (###, **, -) kullanarak mükemmel bir okunabilirlik sağlayın. 'Siz' dilinden asla vazgeçmeyin.
"""


async def extract_filter_params(messages: list) -> dict:
    """Kullanıcı mesaj geçmişinden filtre parametrelerini çıkarır."""
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
    """Arama sonuçlarını yorumlayıp doğal insansı bir yanıt oluşturur."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )

    total = products_result.get("totalElements", 0)
    items = products_result.get("content", [])[:3]

    context_msg = f"""
Sistem Arama Sonuç Özeti:
- Uygulanan Filtreler (JSON): {json.dumps(filter_params, ensure_ascii=False)}
- Bulunan Toplam Ürün Sayısı: {total}
- Örnek Listelenen İlk Ürünler: {[{'isim': i.get('name'), 'fiyat': i.get('price')} for i in items]}

Yönerge: Yukarıdaki sonuç verilerini göz önüne alarak, kullanıcının son mesajına istinaden yönlendirici ve doğal bir Türkçe yanıt üret. Kısa ve öz ol (Maksimum 2-3 paragraf).
"""
    try:
        messages_with_sys = [
            SystemMessage(content=RESPONSE_SYSTEM_PROMPT),
            messages[-1],
            HumanMessage(content=context_msg)
        ]
        response = await llm.ainvoke(messages_with_sys)
        return response.content
    except Exception as e:
        logger.error("conversational_response_error", error=str(e))
        return f"{total} ürün bulundu."


async def generate_detail_response(messages: list, product: dict) -> str:
    """Ürün detay bilgisinden doğal dilde açıklama üretir."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )

    detail_info = {
        "isim": product.get("name", "?"),
        "fiyat": product.get("price", "?"),
        "indirimli_fiyat": product.get("discountedPrice"),
        "açıklama": product.get("description", "Açıklama yok"),
        "kategori": product.get("categoryName", ""),
        "marka": product.get("brand", ""),
        "puan": product.get("ratingAvg", ""),
        "yorum_sayısı": product.get("ratingCount", 0),
        "stok": product.get("stock", ""),
        "renkler": product.get("colors", []),
        "bedenler": product.get("sizes", []),
    }

    context_msg = f"""
Ürün Detay Bilgileri:
{json.dumps(detail_info, ensure_ascii=False, indent=2)}
"""
    try:
        response = await llm.ainvoke([
            SystemMessage(content=DETAIL_RESPONSE_PROMPT),
            messages[-1],
            HumanMessage(content=context_msg),
        ])
        return response.content
    except Exception as e:
        logger.error("detail_response_error", error=str(e))
        name = product.get("name", "Ürün")
        price = product.get("price", "?")
        return f"**{name}** — {price} TL"


async def filter_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Filter Agent node'u.
    Mesaj geçmişinden filtre çıkarır, Spring Boot'a sorgu yapar, doğal dilde yanıt oluşturur.
    PRODUCT_DETAIL intent'i geldiğinde ürün detayı getirir.
    """
    messages = state["messages"]
    user_id = state.get("user_id")
    intent = state.get("intent", "")

    # 1. Filtre parametrelerini çıkar
    params = await extract_filter_params(messages)
    is_detail = params.get("detail_mode", False) or intent == "PRODUCT_DETAIL"

    try:
        # 2. Ürünü ara
        if params.get("q") and not any(k in params for k in ["category", "colors", "sizes", "brand", "sort_by", "min_price", "max_price", "rating"]):
            result = await search_products.ainvoke({
                "query": params["q"],
                "user_id": user_id,
                "page": params.get("page", 0),
                "size": params.get("size", 10),
            })
        else:
            filter_params = {k: v for k, v in params.items() if k != "detail_mode"}
            result = await filter_products.ainvoke({
                **filter_params,
                "user_id": user_id,
            })

        content = result.get("content", [])
        total = result.get("totalElements", 0)

        if "error" in result:
            return {
                **state,
                "final_response": f"Ürünler aranırken geçici bir teknik sorun oluştu: {result['error']}",
                "agent_type": "filter_agent",
                "action_type": "INFO",
            }

        # 3. DETAY MODU: İlk ürünün detayını getir
        if is_detail and content:
            product_id = content[0].get("id")
            if product_id:
                detail = await get_product_detail.ainvoke({
                    "product_id": product_id,
                    "user_id": user_id,
                })

                if "error" not in detail:
                    response_text = await generate_detail_response(messages, detail)
                    return {
                        **state,
                        "final_response": response_text,
                        "agent_type": "filter_agent",
                        "action_type": "PRODUCT_LIST",
                        "action_data": {
                            "filters": params,
                            "products": {"content": [detail], "totalElements": 1},
                            "detail_mode": True,
                        },
                    }

        # 4. Sonuç yoksa
        if not content:
            return {
                **state,
                "final_response": "Aradığınız kriterlere uygun ürün bulunamadı. "
                                  "Filtreleri değiştirmeyi veya farklı bir arama terimi denemeyi önerebilirim.",
                "agent_type": "filter_agent",
                "action_type": "INFO",
            }

        # 5. Normal liste modu: Conversational yanıt üret
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
