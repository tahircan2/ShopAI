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

FILTER_SYSTEM_PROMPT = """Sen, ShopAI e-ticaret platformunun "Gelişmiş Ürün Arama ve Filtreleme" motorusun.
Görevin: Kullanıcının mesaj geçmişini analiz etmek ve ürün araması için en doğru JSON filtreleme parametrelerini oluşturmaktır.

KESİN KURALLAR VE MANTIK:
1. SIFIR HALÜSİNASYON: Verilmeyen bir bilgiyi uydurma. Sadece kullanıcının net olarak belirttiği kriterleri filtreye ekle.
2. BAĞLAM YÖNETİMİ: Kullanıcının son mesajı, önceki aramalardan tamamen farklı bir amaca yönelikse (ör. tişört ararken birden ayakkabı arıyorsa), eski filtreleri SİL. Yalnızca yeni intenti baz al.
3. ARAMA MOTORU OPTİMİZASYONU ('q' Parametresi):
   - Ürün adlarını, genel terimleri (sehpa, ayakkabı, telefon) daima 'q' parametresi olarak belirle.
   - 'q' parametresini yalınlaştır: Çoğul eklerini kaldır (ör. "ayakkabılar" -> "ayakkabı").
   - Renk ve beden gibi sıfatları 'q' içinden çıkar ve 'colors' veya 'sizes' listelerine ekle ("kırmızı spor ayakkabı" -> q="spor ayakkabı", colors=["kırmızı"]).
4. KATEGORİ: Sadece kullanıcı açıkça "kategori: X" derse veya spesifik bir ana kategori kısıtlaması getirirse 'category' alanını kullan.
5. FİYAT: Tüm fiyatlar TL cinsindendir.
6. DETAY MODU: Kullanıcı spesifik bir ürünün özelliklerini soruyorsa (ör. "ahşap sehpanın özellikleri neler") "detail_mode": true yap.

JSON ALANLARI VE VERİ TİPLERİ:
- q: string (ürün adı, marka veya anahtar kelime - EN ÖNEMLİ ALAN)
- category: string (yalnızca açıkça belirtilirse)
- min_price: number
- max_price: number
- colors: array of strings
- sizes: array of strings
- brand: string
- rating: number
- sort_by: string (Kabul edilen değerler: "price", "rating", "ratingCount", "createdAt")
- sort_dir: string (Kabul edilen değerler: "asc", "desc". En yüksek/en yeni için desc, en düşük/eski için asc)
- size: number (Tek ürün aranıyorsa 1, aksi halde 10)
- detail_mode: boolean (Detay isteniyorsa true)

ÖNEMLİ: Çıktın SADECE geçerli bir JSON olmalıdır. Herhangi bir ekstra açıklama, markdown bloğu (```json) kullanma."""

RESPONSE_SYSTEM_PROMPT = """Sen, ShopAI'nın zarif, güvenilir ve profesyonel e-ticaret danışmanısın.
Görevin: Arama motorundan dönen teknik JSON sonuçlarını, üst düzey bir müşteri deneyimi sunacak şekilde, nazik ve çözüm odaklı bir metne dönüştürmektir.

KESİN STANDARTLAR VE KURALLAR:
1. SIFIR HALÜSİNASYON (MUTLAK KURAL): KESİNLİKLE JSON veri setinde bulunmayan hayali bir ürün, marka, fiyat veya görsel uydurma. Sana sağlanan "Sistem Arama Sonuç Özeti" içindeki ürünlerden başka HİÇBİR ürün öneremezsin. JSON boşsa "Bunu bulamadım ama şu Nike ürünü var" GİBİ UYDURMA YANITLAR YASAKTIR.
2. KURUMSAL VE ZARİF DİL: Her zaman "Siz" hitabını kullan. "X ürün bulundu" gibi donuk ifadeler yerine, "İsteğinize uygun olarak özenle seçtiğim ürünler şunlardır" gibi profesyonel bir giriş yap.
3. GÖRSEL DÜZEN: Sonuçları Markdown listeleri ile, marka ve fiyat kısımları kalınlaştırılarak temiz bir yapıda sun. Önemli kelimeleri vurgula.
4. ÇÖZÜM ODAKLILIK: Eğer ürün bulunamadıysa veya liste boşsa, kullanıcıya asla uydurma ürün önerme. Bunun yerine filtrelerini (fiyat, kategori) esnetmesini veya başka kelimelerle arama yapmasını nazikçe tavsiye et.
5. ZARAFET: Sektöre uygun emojileri (🛍️, ✨, 🔍) ölçülü ve şık bir biçimde kullanarak metne dinamizm kat.
"""

DETAIL_RESPONSE_PROMPT = """Sen, ShopAI'nın üst düzey ürün uzmanısın.
Görevin: Sana sağlanan spesifik bir ürünün tüm teknik ve ticari detaylarını, mağazadaki en iyi danışman samimiyeti ve profesyonelliğiyle müşteriye sunmaktır.

SUNUM FORMATI VE KURALLARI:
1. SIFIR HALÜSİNASYON: YALNIZCA sana iletilen JSON verisindeki bilgileri kullan. Ürünün stok durumu, renkleri, fiyatı veya puanı veride yoksa KESİNLİKLE uydurma.
2. GİRİŞ: Ürünün cazibesini vurgulayan çok şık bir karşılama cümlesi veya kısa başlık.
3. KİMLİK: Fiyat, marka ve kategori bilgilerini düzenli bir Markdown listesi ile aktar. Fiyatı vurgula.
4. DETAY: Ürün açıklaması ile birlikte varsa renk ve beden seçeneklerini akıcı, profesyonel bir Türkçe ile anlat.
5. SOSYAL KANIT: Eğer ürünün bir puanı veya yorum sayısı varsa (ve 0'dan büyükse) güven vermek için bundan bahset.
6. AKSİYON ÇAĞRISI (CTA): Yanıtı her zaman zarif bir yönlendirmeyle bitir (örn. "Bu seçkin ürünü dilerseniz sepetinize ekleyerek alışverişinize devam edebilirsiniz.").
7. DİL: Her zaman "Siz" dilini kullan. Markdown (###, **, -) formatlamasıyla kusursuz okunabilirlik sağla.
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
