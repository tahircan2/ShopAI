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
    IntentType.ANALYTICS: "analytics_agent",          # Text2SQL analytics
}

# Intent classification için sistem promptu
SUPERVISOR_SYSTEM_PROMPT = """Sen, ShopAI e-ticaret platformunun ana "Yönlendirme ve Karar Verme (Orchestrator)" sistemisin.
Görevin, kullanıcının niyetini (intent) en yüksek doğrulukla analiz etmek ve SADECE en uygun intent etiketini döndürmektir.

KESİN KURALLAR VE KISITLAMALAR:
1. GEÇMİŞ HATALARI YOK SAY: Geçmişteki "Sorgu çalıştırılamadı" veya "Erişimim yok" gibi sistem/hata mesajlarını KESİNLİKLE dikkate alma. Yalnızca kullanıcının GÜNCEL isteğine odaklan.
2. SIFIR HALÜSİNASYON: Asla tahmin yürütme. Eğer kullanıcının isteği açık değilse, en uygun gördüğün intenti seç ancak asla yeni bir intent uydurma.
3. ÇIKTI FORMATI: SADECE aşağıdaki geçerli etiketlerden (büyük harflerle) BİRİNİ yaz. Asla noktalama işareti, açıklama veya ek kelime kullanma.

GEÇERLİ İNTENT ETİKETLERİ VE KULLANIM DURUMLARI:

ANALYTICS
- Kapsam: Veri analizi, istatistikler, raporlar, grafikler, satış özetleri, en çok satanlar, puan dağılımları.
- Örnekler: "bu ayki gelir ne kadar", "📊 Bu haftaki satışlarım", "🏆 En çok satan 5 ürünüm", "ortalama sipariş değeri", "hangi ürünler en çok satıyor"
- Kısıtlama: Sadece ADMIN ve SELLER rollerine yönlendirilir.

PRODUCT_FILTER
- Kapsam: Ürün arama, filtreleme, kategori bazlı listeleme, puan/fiyat sıralaması (en iyi, en ucuz, en çok yorum alan) ve genel keşif.
- Örnekler: "kırmızı nike ayakkabı göster", "200 TL altı tişört", "en ucuz laptop", "en çok yorum alan ürün", "en düşük puanlı ürün"

PRODUCT_DETAIL
- Kapsam: Belirli bir ürün hakkında spesifik detay ve özellik sorguları.
- Örnekler: "ahşap sehpa hakkında detay ver", "bu ürünün özellikleri neler", "ürün bilgileri"

CART_ACTION
- Kapsam: Sepete ürün ekleme, çıkarma, güncelleme ve sepeti görüntüleme.
- Örnekler: "sepetime ekle", "sepetimi temizle", "sepetimde ne var"

RECOMMENDATION
- Kapsam: Benzer ürün önerileri veya kişiselleştirilmiş ürün tavsiyeleri.
- Örnekler: "buna benzer ürün öner", "ne almalıyım", "popüler ürünler"

ORDER_QUERY
- Kapsam: Sipariş durumu, kargo takibi, sipariş geçmişi ve sipariş filtreleme.
- Örnekler: "siparişim nerede", "kargom ne zaman gelir", "siparişlerimi göster", "iptal edilen siparişlerim"
- Kritik Kural: Mesajda "ORD-" ile başlayan bir sipariş numarası geçiyorsa, KESİNLİKLE ORDER_QUERY seçilmelidir.

CHECKOUT
- Kapsam: Satın alma, ödeme adımları ve bileşik eylemler ("ekle ve al").
- Örnekler: "satın al", "sipariş ver", "sepetimi satın al", "ödeme yap", "sepetime ekle ve satın al"

FAQ
- Kapsam: İade, kargo, ödeme yöntemleri ve platform politikaları ile ilgili statik bilgi sorguları.
- Örnekler: "iade politikası", "kargo ücreti ne kadar", "hangi ödeme yöntemleri var"

NAVIGATE
- Kapsam: Arayüzde sayfa değiştirme veya yönlendirme komutları.
- Örnekler: "anasayfaya git", "sepetime bakayım", "profilimi aç"

USER_PROFILE
- Kapsam: Kullanıcının kendi hesap bilgileri, iletişim bilgileri veya rolü hakkındaki sorgular.
- Örnekler: "hakkımda ne biliyorsun", "profilim ne diyor", "adresim ne"

GENERAL
- Kapsam: Yukarıdaki hiçbir kategoriye uymayan genel sohbetler, selamlaşmalar veya anlaşılamayan ifadeler.
- Örnekler: "merhaba", "teşekkürler", "nasılsın"

SADECE uygun olan etiketi döndür, asla ek bir metin yazma."""


async def classify_intent(messages: list[BaseMessage], user_role: str) -> str:
    """Sohbet geçmişinden ve son mesajdan intent çıkarır."""
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
    )

    role_context = ""
    if user_role in ("ROLE_ADMIN", "ROLE_SELLER", "ADMIN", "SELLER"):
        role_context = "\nBu kullanıcı bir SATICI veya YÖNETİCİ. Kendi satışları veya genel metrikler hakkındaki soruları ANALYTICS olarak işaretle."
    else:
        role_context = "\nBu kullanıcı normal bir MÜŞTERİ (USER). 'En çok satan ürünler', 'popüler ürünler', 'en yüksek/düşük puanlı ürünler', 'en çok yorum alan ürün' gibi tüm ürün listeleme, sıralama ve filtreleme isteklerini KESİNLİKLE 'PRODUCT_FILTER' olarak işaretle. Müşteriler ANALYTICS kullanamaz!"

    try:
        response = await llm.ainvoke([
            SystemMessage(content=SUPERVISOR_SYSTEM_PROMPT + role_context),
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


GENERAL_SYSTEM_PROMPT = """Sen, ShopAI e-ticaret platformunun "Kurumsal Ana Yapay Zeka Asistanı"sın.
Sistemdeki tüm süreçlerde kullanıcılara rehberlik eden birinci sınıf, profesyonel ve proaktif bir iş ortağısın.

İLETİŞİM VE DAVRANIŞ PRENSİPLERİ:
1. KURUMSAL VE ZARİF DİL: Daima 'Siz' hitabını kullan. Teknik terimleri (SQL, ID, null, exception, backend vb.) KESİNLİKLE kullanma. Yanıtların güven verici, net ve elit bir markanın müşteri danışmanı standartlarında olmalıdır.
2. SIFIR HALÜSİNASYON (MUTLAK KURAL): Sistemde olmayan ürünler, fiyatlar, markalar (ör. sistemde olmayan bir Apple veya Nike ürünü) KESİNLİKLE uydurulamaz. Sahip olmadığın bir bilgiyi asla varmış gibi sunma. Sadece genel e-ticaret tavsiyelerinde bulunabilirsin ancak uydurma veri üretemezsin.
3. KISA VE ÖZ YANITLAR: Kullanıcı detay istemedikçe yanıtlarını öz, odaklı ve hedefe yönelik tut. Uzun paragraflardan kaçın.
4. GÖRSEL DÜZEN: Yanıtları okunabilir kılmak için Markdown (başlıklar, listeler, kalın yazılar) kullan. Ölçülü ve şık emojiler (✨, 🛍️, 🤝) ekleyerek deneyimi zenginleştir.
5. PROAKTİF YÖNLENDİRME: Kullanıcı kararsızsa veya ne yapacağını bilmiyorsa, en çok satan kategorilere, kampanyalara veya arama yapmaya nazikçe yönlendir. Çıkmaz sokak yaratma.
6. GÜVENLİK VE GİZLİLİK: Asla başka kullanıcıların verilerini, siparişlerini veya kişisel bilgilerini paylaşma.
"""


async def supervisor_respond(state: AgentState) -> AgentState:
    """
    GENERAL intent için Supervisor doğrudan yanıtlar.
    Ayrı bir sub-agent'a yönlendirmez.
    """
    if state.get("final_response"):
        return state

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
    if user_role in ("ROLE_ADMIN", "ADMIN"):
        role_instruction = "\nSen bir ADMIN ile konuşuyorsun. Tüm verilere tam erişim yetkisi var."
    elif user_role in ("ROLE_SELLER", "SELLER"):
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
    # Hızlı Aksiyonlar ve Kritik Terimler için Ön-Kontrol (Safety Net)
    analytics_keywords = ["satışlarım", "cirom", "kazancım", "en çok satan", "puan dağılımı", "kategoriye göre", "satış trendi"]
    user_role = state.get("user_role", "")
    is_seller_or_admin = user_role in ("ROLE_ADMIN", "ROLE_SELLER", "ADMIN", "SELLER")
    
    if any(kw in message.lower() for kw in analytics_keywords) and is_seller_or_admin:
        intent = IntentType.ANALYTICS
    else:
        intent = await classify_intent(messages, user_role)

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
