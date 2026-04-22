"""
agents/supervisor.py — Intent classification ve agent routing.

Supervisor, kullanıcı mesajını analiz eder ve doğru sub-agent'a yönlendirir.
Kullanıcı girdisi ASLA sistem promptuna string concatenation ile eklenmez.
Her zaman ayrı HumanMessage nesnesi olarak iletilir.

Desteklenen intent'ler:
  PRODUCT_FILTER — Ürün arama, filtreleme, listeleme
  PRODUCT_DETAIL — Belirli bir ürün hakkında detaylı bilgi
  CART_ACTION — Sepet işlemleri
  RECOMMENDATION — Ürün önerisi
  ORDER_QUERY — Sipariş sorgulama (filtreleme dahil)
  CHECKOUT — Satın alma (bileşik "ekle ve satın al" dahil)
  FAQ — SSS
  NAVIGATE — Sayfa yönlendirme
  USER_PROFILE — Kullanıcı bilgisi sorgulama
  GENERAL — Genel sohbet
"""

import structlog
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, BaseMessage

from config import settings
from graph.state import AgentState
from models.schemas import IntentType
from tools.user_tools import get_user_profile

logger = structlog.get_logger(__name__)

# Intent → Agent mapping
INTENT_TO_AGENT: dict[str, str] = {
    IntentType.PRODUCT_FILTER: "filter_agent",
    IntentType.PRODUCT_DETAIL: "filter_agent",       # filter_agent detay modunda çalışır
    IntentType.CART_ACTION: "cart_agent",
    IntentType.RECOMMENDATION: "recommend_agent",
    IntentType.ORDER_QUERY: "order_agent",
    IntentType.FAQ: "faq_agent",
    IntentType.CHECKOUT: "checkout_agent",
    IntentType.NAVIGATE: "navigation_agent",
    IntentType.USER_PROFILE: "supervisor_profile",    # Supervisor profil yanıtı verir
    IntentType.GENERAL: "supervisor",                 # Supervisor doğrudan yanıtlar
}

# Intent classification için sistem promptu
SUPERVISOR_SYSTEM_PROMPT = """Sen ShopAI e-ticaret platformunun yönlendirme sistemisin.
Kullanıcı mesajını analiz et ve YALNIZCA aşağıdaki intent etiketlerinden birini döndür.
Başka hiçbir şey yazma — sadece etiket:

PRODUCT_FILTER — Ürün arama, filtreleme, listeleme
  Örnekler: "kırmızı nike ayakkabı göster", "200 TL altı tişört", "en ucuz laptop", "sehpa bul"

PRODUCT_DETAIL — Belirli bir ürün hakkında detaylı bilgi isteme
  Örnekler: "ahşap sehpa hakkında detay ver", "bu ürünün özellikleri neler", "ürün bilgileri"

CART_ACTION — Sepet işlemleri (ekle, çıkar, güncelle, görüntüle)
  Örnekler: "sepetime ekle", "sepetimi temizle", "sepetimde ne var"

RECOMMENDATION — Ürün önerisi, benzer ürün
  Örnekler: "buna benzer ürün öner", "ne almalıyım", "popüler ürünler"

ORDER_QUERY — Sipariş durumu, kargo takibi, sipariş geçmişi, sipariş filtreleme
  Örnekler: "siparişim nerede", "kargom ne zaman gelir", "siparişlerimi göster",
  "kargoya verilen siparişlerim", "22 Nisan tarihli siparişlerim", "iptal edilen siparişlerim",
  "ORD-20260422-8372 nolu siparişimde ne var"
  ÖNEMLİ: Eğer mesajda ORD- ile başlayan bir sipariş numarası varsa, ürün detayı sorsa bile ORDER_QUERY seç.

CHECKOUT — Satın alma, ödeme, sipariş tamamlama, bileşik sepet+ödeme işlemleri
  Örnekler: "satın al", "sipariş ver", "sepetimi satın al", "ödeme yap",
  "sepetime ekle ve satın al", "hemen al", "kapıda ödeme ile satın al"
  NOT: "sepetime ekle ve satın al" gibi bileşik istekler CHECKOUT olarak sınıflandırılır.

FAQ — Sık sorulan sorular (kargo, iade, ödeme, politika)
  Örnekler: "iade politikası", "kargo ücreti ne kadar", "hangi ödeme yöntemleri var"

NAVIGATE — Sitenin farklı bölümlerine gitme, sayfalar arası geçiş
  Örnekler: "anasayfaya git", "sepetime bakayım", "profilimi aç", "siparişlerim sayfasına git"

USER_PROFILE — Kullanıcının kendi bilgileri hakkında soru sorması
  Örnekler: "hakkımda ne biliyorsun", "profilim ne diyor", "hangi bilgilerim kayıtlı",
  "adresim ne", "e-posta adresim nedir", "hesap bilgilerim"

GENERAL — Yukarıdakilerden hiçbirine girmeyen genel sorular
  Örnekler: "merhaba", "teşekkürler", "nasılsın"

Sadece etiket yaz. Açıklama ekleme."""


async def classify_intent(messages: list[BaseMessage]) -> str:
    """Sohbet geçmişinden ve son mesajdan intent çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=SUPERVISOR_SYSTEM_PROMPT),
            *messages,
        ])

        intent_raw = response.content.strip().upper()
        # Eğer LLM sadece etiketi değil de bir cümle kurarsa ilk kelimeyi almayı dene
        if "\n" in intent_raw:
            intent_raw = intent_raw.split("\n")[0].strip()

        logger.info("intent_classified", intent=intent_raw)

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
Kullanıcılara şu konularda aktif destek verirsin:
- Ürün arama, filtreleme ve detaylı bilgi
- Sepet işlemleri ve satın alma (checkout) süreçleri
- Sipariş takibi ve geçmiş siparişlerin sorgulanması
- Kargo, iade ve ödeme yöntemleri hakkındaki sorular

Eğer bir işlem için onay bekliyorsak, kullanıcıyı o onay kartına yönlendir.
Ödeme ve satın alma işlemlerinde yetkili agent'lara (checkout_agent) yönlendirme yapabilirsin.
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

    # Rol tabanlı yönlendirme
    user_role = state.get("user_role")
    role_instruction = ""
    if user_role == "ROLE_ADMIN":
        role_instruction = "\nSen bir ADMIN ile konuşuyorsun. Tüm verilere tam erişim yetkisi var."
    elif user_role == "ROLE_SELLER":
        role_instruction = "\nKullanıcı bir SATICI (SELLER). Sadece kendi satışları ve ürünleriyle ilgili özel bilgiler verebilir."
    else:
        role_instruction = "\nKullanıcı normal bir USER'dır. Başkalarına ait özel bilgileri veya siparişleri KESİNLİKLE paylaşma."

    try:
        response = await llm.ainvoke([
            SystemMessage(content=GENERAL_SYSTEM_PROMPT + role_instruction),
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


async def supervisor_profile_respond(state: AgentState) -> AgentState:
    """
    USER_PROFILE intent için kullanıcı bilgilerini getirir ve yanıtlar.
    """
    user_id = state.get("user_id")

    if not user_id:
        return {
            **state,
            "final_response": "Profil bilgilerinizi görmek için giriş yapmanız gerekiyor.",
            "agent_type": "supervisor",
            "action_type": "INFO",
        }

    # Kullanıcı profilini getir
    profile = await get_user_profile.ainvoke({"user_id": user_id})

    if "error" in profile:
        return {
            **state,
            "final_response": profile["error"],
            "agent_type": "supervisor",
            "action_type": "INFO",
        }

    # Profil bilgilerini doğal dilde sun
    name = profile.get("firstName", "") or ""
    surname = profile.get("lastName", "") or ""
    email = profile.get("email", "")
    role = profile.get("role", "USER")
    phone = profile.get("phone", "")

    role_labels = {
        "ROLE_ADMIN": "Yönetici",
        "ROLE_SELLER": "Satıcı",
        "ROLE_USER": "Kullanıcı",
        "ADMIN": "Yönetici",
        "SELLER": "Satıcı",
        "USER": "Kullanıcı",
    }
    role_label = role_labels.get(role, role)

    lines = [f"İşte kayıtlı bilgileriniz:\n"]
    if name or surname:
        lines.append(f"👤 **Ad Soyad:** {name} {surname}".strip())
    if email:
        lines.append(f"📧 **E-posta:** {email}")
    if phone:
        lines.append(f"📱 **Telefon:** {phone}")
    lines.append(f"🏷️ **Hesap Tipi:** {role_label}")

    response_text = "\n".join(lines)

    return {
        **state,
        "final_response": response_text,
        "agent_type": "supervisor",
        "action_type": "INFO",
        "action_data": {"profile": profile},
    }


async def supervisor_node(state: AgentState) -> AgentState:
    """
    LangGraph Supervisor node'u.
    Intent classification yapar, agent seçer.
    """
    import re
    from tools.approval_tools import get_latest_approval

    message = state["current_message"]
    messages = state["messages"]
    user_id = state.get("user_id")

    # Regex ile onay ifadelerini yakala (onayladım, onaylıyorum, APPROVED, devam et, buy it vb.)
    approval_keywords = [
        r"\bapproved\b", r"\bonay\b", r"\bonaylıyorum\b", r"\bonayladım\b", 
        r"\bdevam\s+et\b", r"\bsatın\s+almayı\s+tamamla\b"
    ]
    rejection_keywords = [
        r"\brejected\b", r"\bred\b", r"\breddet\b", r"\biptal\b", r"\biptal\s+et\b", r"\bvazgeçtim\b"
    ]
    
    is_approval_signal = any(re.search(kw, message, re.IGNORECASE) for kw in approval_keywords)
    is_rejection_signal = any(re.search(kw, message, re.IGNORECASE) for kw in rejection_keywords)

    if is_approval_signal or message == "APPROVED" or state.get("approval_status") == "APPROVED":
        logger.info("approval_signal_detected", message=message)
        
        # Eğer state'de plan yoksa backend'den çekmeye çalış
        if not state.get("plan_data") and user_id:
            latest = await get_latest_approval(user_id)
            if latest:
                token = latest.get("approvalToken")
                status = latest.get("status")
                
                # Eğer durum hala PENDING ise ve kullanıcı onay mesajı verdiyse, 
                # backend'de APPROVED olarak işaretle
                if status == "PENDING" and token:
                    from tools.approval_tools import approve_transaction
                    await approve_transaction(user_id, token)
                    status = "APPROVED"

                logger.info("recovering_approval_context", token=token, status=status)
                return {
                    **state,
                    "intent": "MULTI_STEP",
                    "selected_agent": "multi_step_executor",
                    "approval_token": token,
                    "plan_data": latest.get("planData"),
                    "approval_status": status,
                }
            else:
                # Token bulunamadı veya süresi dolmuş
                return {
                    **state,
                    "final_response": "Onay süresi dolmuş veya bekleyen bir işlem bulunamadı. Lütfen işlemi tekrar başlatın (örneğin: 'sepetimi satın al').",
                    "intent": "GENERAL",
                    "selected_agent": "supervisor",
                    "action_type": "INFO",
                }

        # State'de plan varsa (zaten akışın içindeyiz)
        return {
            **state,
            "intent": "MULTI_STEP",
            "selected_agent": "multi_step_executor",
        }

    # Rejection Handling
    if is_rejection_signal or message == "REJECTED":
        logger.info("rejection_signal_detected", message=message)
        
        # Backend'de REJECTED olarak işaretle
        if user_id:
            from tools.approval_tools import get_latest_approval, reject_transaction
            latest = await get_latest_approval(user_id)
            if latest and latest.get("status") == "PENDING":
                token = latest.get("approvalToken")
                if token:
                    await reject_transaction(user_id, token)

        return {
            **state,
            "final_response": "Anladım, işlemi iptal ettim. Başka bir konuda yardımcı olabilir miyim?",
            "intent": "GENERAL",
            "selected_agent": "supervisor",
            "approval_status": "REJECTED",
            "requires_approval": False,
            "plan_data": None,
            "action_type": "INFO",
        }

    # Normal Intent classification (Tüm geçmiş ile)
    intent = await classify_intent(messages)

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
