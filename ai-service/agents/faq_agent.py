"""
agents/faq_agent.py — SSS (Sık Sorulan Sorular) agent'ı.

Kargo, iade, ödeme ve platform politikaları hakkındaki soruları yanıtlar.
Statik bilgi tabanı kullanır — Spring Boot API'ye ihtiyaç duymaz.
"""

import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState

logger = structlog.get_logger(__name__)

# ShopAI politika bilgi tabanı
FAQ_KNOWLEDGE_BASE = """
# ShopAI Platform Politikaları

## Kargo
- Standart kargo süresi: 2-5 iş günü
- Kargo ücreti: 29.99 TL (sabit)
- 500 TL ve üzeri siparişlerde kargo ÜCRETSİZ
- Kargo firması: Yurt içi tüm iller için anlaşmalı kargo firmamız
- Kargo takibi: Sipariş onayı sonrası e-posta ve bildirim ile iletilir
- Aynı gün kargo: Saat 14:00'dan önce verilen siparişler aynı gün kargoya verilir (hafta içi)

## İade Politikası
- İade süresi: Teslim tarihinden itibaren 14 gün
- İade koşulları: Ürün kullanılmamış, orijinal ambalajında ve tüm aksesuarlarıyla iade edilmelidir
- İade yöntemi: Kargo ile gönderim (iade kargosu müşteriye aittir, 1. iade ücretsiz)
- İade süreci: İade talebi oluşturduktan sonra 3-5 iş günü içinde para iadesi yapılır
- İade edilemeyen ürünler: İç çamaşırı, makyaj, gıda ürünleri (hijyen nedeniyle)
- Hasarlı veya hatalı ürünlerde iade kargosu ShopAI tarafından karşılanır

## Ödeme Yöntemleri
- Kredi kartı (Visa, Mastercard, American Express)
- Banka kartı (Debit card)
- Havale/EFT (sipariş onayı sonrası 24 saat içinde ödeme yapılmalı)
- Kapıda ödeme (nakit veya kredi kartı ile, +10 TL hizmet bedeli)
- Taksit seçenekleri: 3, 6, 9, 12 taksit (katılımcı bankalar ile)

## Hesap & Üyelik
- Kayıt ücretsizdir
- E-posta doğrulaması zorunludur
- Şifre: en az 8 karakter, büyük/küçük harf ve rakam içermelidir
- Hesap silme: Müşteri hizmetleri üzerinden talep edilebilir

## Kampanya & Kupon
- Kupon kodu checkout sırasında girilir
- Her siparişte yalnızca 1 kupon kullanılabilir
- Kampanyalar birleştirilemez (indirimli ürün + kupon uygulanamaz)

## Müşteri Hizmetleri
- E-posta: destek@shopai.com
- Çalışma saatleri: Hafta içi 09:00-18:00
- Yanıt süresi: E-posta ile 24 saat içinde
"""

FAQ_SYSTEM_PROMPT = f"""Sen ShopAI e-ticaret platformunun müşteri hizmetleri asistanısın.
Yalnızca aşağıdaki bilgi tabanındaki bilgileri kullanarak yanıt ver.
Bilgi tabanında olmayan konular hakkında tahmin yürütme — müşteri hizmetlerine yönlendir.
Kısa, net ve samimi yanıtlar ver. Türkçe yaz.

{FAQ_KNOWLEDGE_BASE}

Bilgi tabanında olmayan bir soru gelirse:
"Bu konuda kesin bilgi veremem. destek@shopai.com adresinden müşteri hizmetlerimize ulaşabilirsiniz." de."""


async def faq_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph FAQ Agent node'u.
    Platform politikaları hakkındaki soruları statik bilgi tabanıyla yanıtlar.
    """
    message = state["current_message"]
    history = state.get("messages", [])[-settings.conversation_history_limit:]

    logger.info("faq_agent", message_preview=message[:50])

    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.3,
        api_key=settings.openai_api_key,
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=FAQ_SYSTEM_PROMPT),
            *history,
            HumanMessage(content=message),
        ])

        return {
            **state,
            "final_response": response.content,
            "agent_type": "faq_agent",
            "action_type": "INFO",
            "action_data": None,
        }

    except Exception as e:
        logger.error("faq_agent_error", error=str(e))
        return {
            **state,
            "final_response": (
                "Sorunuzu yanıtlayamadım. Lütfen destek@shopai.com adresinden "
                "müşteri hizmetlerimize ulaşın."
            ),
            "agent_type": "faq_agent",
            "error": str(e),
        }
