"""
agents/supervisor.py — Intent classification ve agent routing.

Supervisor, kullanıcı mesajını analiz eder ve doğru sub-agent'a yönlendirir.
Kullanıcı girdisi ASLA sistem promptuna string concatenation ile eklenmez.
Her zaman ayrı HumanMessage nesnesi olarak iletilir.
"""

import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from config import settings
from graph.state import AgentState
from models.schemas import IntentType

logger = structlog.get_logger(__name__)

# Intent → Agent mapping
INTENT_TO_AGENT: dict[str, str] = {
    IntentType.PRODUCT_FILTER: "filter_agent",
    IntentType.CART_ACTION: "cart_agent",
    IntentType.RECOMMENDATION: "recommend_agent",
    IntentType.ORDER_QUERY: "order_agent",
    IntentType.FAQ: "faq_agent",
    IntentType.GENERAL: "supervisor",  # Supervisor doğrudan yanıtlar
}

# Intent classification için sistem promptu
SUPERVISOR_SYSTEM_PROMPT = """Sen ShopAI e-ticaret platformunun yönlendirme sistemisin.
Kullanıcı mesajını analiz et ve YALNIZCA aşağıdaki intent etiketlerinden birini döndür.
Başka hiçbir şey yazma — sadece etiket:

PRODUCT_FILTER — Ürün arama, filtreleme, listeleme
  Örnekler: "kırmızı nike ayakkabı göster", "200 TL altı tişört", "en ucuz laptop"

CART_ACTION — Sepet işlemleri (ekle, çıkar, güncelle, görüntüle)
  Örnekler: "sepetime ekle", "sepetimi temizle", "sepetimde ne var"

RECOMMENDATION — Ürün önerisi, benzer ürün
  Örnekler: "buna benzer ürün öner", "ne almalıyım", "popüler ürünler"

ORDER_QUERY — Sipariş durumu, kargo takibi
  Örnekler: "siparişim nerede", "kargom ne zaman gelir", "son siparişim"

FAQ — Sık sorulan sorular (kargo, iade, ödeme, politika)
  Örnekler: "iade politikası", "kargo ücreti ne kadar", "hangi ödeme yöntemleri var"

GENERAL — Yukarıdakilerden hiçbirine girmeyen genel sorular
  Örnekler: "merhaba", "teşekkürler", "nasılsın"

Sadece etiket yaz. Açıklama ekleme."""


async def classify_intent(message: str) -> str:
    """
    Kullanıcı mesajından intent çıkarır.

    KRİTİK: message ASLA sistem promptuna concat edilmez.
    HumanMessage olarak ayrı iletilir.

    Args:
        message: Ham kullanıcı mesajı

    Returns:
        IntentType string değeri
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=SUPERVISOR_SYSTEM_PROMPT),
            HumanMessage(content=message),  # ASLA f-string ile concat etme
        ])

        intent_raw = response.content.strip().upper()
        logger.info("intent_classified", intent=intent_raw, message_preview=message[:50])

        # Geçerli intent mi kontrol et
        valid_intents = {e.value for e in IntentType}
        if intent_raw in valid_intents:
            return intent_raw

        # Geçersizse GENERAL döner
        logger.warning("unknown_intent", intent_raw=intent_raw)
        return IntentType.GENERAL

    except Exception as e:
        logger.error("intent_classification_error", error=str(e))
        return IntentType.GENERAL


GENERAL_SYSTEM_PROMPT = """Sen ShopAI e-ticaret platformunun yardımcı asistanısın.
Yalnızca bu platform ile ilgili konularda yardımcı olursun:
- Ürün arama ve filtreleme
- Sepet yönetimi
- Sipariş durumu
- Kargo, iade ve ödeme soruları

Bu konular dışında yardımcı olamazsın. Kibarca reddet ve platform konularında yönlendir.
Kısa, net ve samimi yanıtlar ver. Türkçe ve İngilizce anlarsın, kullanıcının dilinde yanıtla."""


async def supervisor_respond(state: AgentState) -> AgentState:
    """
    GENERAL intent için Supervisor doğrudan yanıtlar.
    Ayrı bir sub-agent'a yönlendirmez.
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0.7,
        api_key=settings.openai_api_key,
        tags=["stream_to_user"]
    )

    # Konuşma geçmişini dahil et (son N mesaj)
    history = state["messages"][-settings.conversation_history_limit:]

    try:
        response = await llm.ainvoke([
            SystemMessage(content=GENERAL_SYSTEM_PROMPT),
            *history,
        ])

        return {
            **state,
            "final_response": response.content,
            "agent_type": "supervisor",
            "action_type": "INFO",
            "action_data": None,
        }
    except Exception as e:
        logger.error("supervisor_respond_error", error=str(e))
        return {
            **state,
            "final_response": "Bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "supervisor",
            "error": str(e),
        }


async def supervisor_node(state: AgentState) -> AgentState:
    """
    LangGraph Supervisor node'u.
    Intent classification yapar, agent seçer.
    """
    message = state["current_message"]

    # Intent classification
    intent = await classify_intent(message)

    # Agent seç
    selected_agent = INTENT_TO_AGENT.get(intent, "supervisor")

    logger.info(
        "supervisor_routing",
        intent=intent,
        selected_agent=selected_agent,
        user_id=state.get("user_id"),
        session_id=state.get("session_id"),
    )

    return {
        **state,
        "intent": intent,
        "selected_agent": selected_agent,
    }


def route_to_agent(state: AgentState) -> str:
    """
    LangGraph conditional edge fonksiyonu.
    selected_agent değerine göre bir sonraki node'u döner.
    """
    agent = state.get("selected_agent", "supervisor")
    logger.debug("routing_to", agent=agent)
    return agent
