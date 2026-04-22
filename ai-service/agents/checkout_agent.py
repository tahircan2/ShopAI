"""
agents/checkout_agent.py — Agentic Checkout Agent.

Kullanıcının sepetindeki ürünleri satın almasını sağlar.
Çok adımlı bir flow izler:
  1. Sepet doğrulama (boş mu, stok var mı?)
  2. Adres ve ödeme bilgilerini toplama
  3. Kullanıcıdan onay isteme (approval_token ile)
  4. Onay sonrası siparişi oluşturma

KRİTİK: user_id YALNIZCA state'ten (JWT → Spring Boot → Python) alınır.
Kullanıcı girdisinden ASLA alınmaz.
"""

import json
import structlog
import httpx
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, BaseMessage

from config import settings
from graph.state import AgentState
from tools.cart_tools import get_cart
from agents.pre_validation_agent import pre_validation_agent

logger = structlog.get_logger(__name__)

# Spring Boot internal API base
INTERNAL_API = f"{settings.spring_boot_base_url}/internal/agent"
APPROVAL_API = f"{INTERNAL_API}/approvals/create"


async def _internal_get(path: str) -> dict | None:
    """Internal API'ye GET isteği gönderir (X-Internal-Key header ile)."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{INTERNAL_API}{path}",
                headers={"X-Internal-Key": settings.spring_boot_internal_key},
            )
            if resp.status_code == 200:
                return resp.json()
            elif resp.status_code == 404:
                return None
            else:
                logger.warning("internal_api_error", path=path, status=resp.status_code)
                return None
    except Exception as e:
        logger.error("internal_api_exception", path=path, error=str(e))
        return None


async def _create_approval(user_id: str, plan_data: str, session_id: str) -> dict | None:
    """Spring Boot'a onay planı oluşturma isteği gönderir."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                APPROVAL_API,
                json={
                    "planData": plan_data,
                    "agentType": "checkout_orchestration",
                    "sessionId": session_id,
                },
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id),
                },
                cookies={},  # Internal call — cookie gerekli değil
            )
            if resp.status_code == 200:
                return resp.json()
            else:
                logger.warning("approval_create_error", status=resp.status_code, body=resp.text)
                return None
    except Exception as e:
        logger.error("approval_create_exception", error=str(e))
        return None


async def _extract_product_from_message(messages: list[BaseMessage]) -> str | None:
    """
    Bileşik intent'lerde mesajdan ürün adını çıkarır.
    Örn: 'ahşap sehpayı sepetime ekle ve satın al' → 'ahşap sehpa'
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        temperature=0,
        api_key=settings.openai_api_key,
    )

    try:
        response = await llm.ainvoke([
            SystemMessage(content=(
                "Kullanıcı mesajında 'sepetime ekle ve satın al' gibi bileşik bir istek varsa, "
                "eklenmek istenen ürünün adını SADECE ürün adı olarak döndür. "
                "Eğer ürün adı yoksa veya mesaj bileşik değilse, 'NONE' döndür. "
                "Başka hiçbir şey yazma."
            )),
            messages[-1] if messages else SystemMessage(content=""),
        ])
        result = response.content.strip()
        if result.upper() == "NONE" or len(result) < 2:
            return None
        return result
    except Exception as e:
        logger.error("extract_product_error", error=str(e))
        return None


async def checkout_agent_node(state: AgentState) -> AgentState:
    """
    LangGraph Checkout Agent node'u.

    Flow:
      1. Giriş kontrolü
      2. Bileşik intent kontrolü (ürün ekle + satın al)
      3. Sepet kontrolü (boş mu?)
      4. Adres ve ödeme bilgisi kontrolü
      5. Kullanıcıya onay planı sunma (action_required)
    """
    user_id = state.get("user_id")
    session_id = state.get("session_id", "")
    message = state.get("current_message", "")

    # ── 1. Giriş kontrolü ──
    if not user_id or str(user_id).strip() == "":
        return {
            **state,
            "final_response": "Satın alma işlemi için giriş yapmanız gerekiyor. "
                              "Sağ üstten giriş yapabilirsiniz.",
            "agent_type": "checkout_agent",
            "action_type": "INFO",
        }

    # ── 2. Sepet kontrolü ──
    cart = await get_cart.ainvoke({"user_id": user_id})

    if "error" in cart:
        return {
            **state,
            "final_response": f"Sepetinize erişilemiyor: {cart['error']}",
            "agent_type": "checkout_agent",
            "action_type": "INFO",
        }

    items = cart.get("items", [])

    # ── 2b. Bileşik intent: Sepet boşsa ve mesajda ürün adı varsa, önce ekle ──
    if not items:
        product_to_add = await _extract_product_from_message(state["messages"])
        if product_to_add:
            from tools.product_tools import search_products
            from tools.cart_tools import add_to_cart as add_tool
            result = await search_products.ainvoke({
                "query": product_to_add,
                "user_id": user_id,
                "page": 0,
                "size": 1,
            })
            products = result.get("content", [])
            if products:
                add_result = await add_tool.ainvoke({
                    "user_id": user_id,
                    "product_id": products[0]["id"],
                    "quantity": 1,
                })
                if "error" not in add_result:
                    cart = add_result
                    items = cart.get("items", [])
                    logger.info("compound_intent_product_added",
                                product=products[0].get("name"), user_id=user_id)

    if not items:
        return {
            **state,
            "final_response": "Sepetiniz boş! Önce ürün ekleyerek başlayın. "
                              "Örneğin: 'Nike ayakkabı sepetime ekle'",
            "agent_type": "checkout_agent",
            "action_type": "INFO",
        }

    # ── 3. Adres ve tercih bilgilerini getir ──
    address = await _internal_get(f"/user/{user_id}/default-address")
    preferences = await _internal_get(f"/user/{user_id}/preferences")

    # ── 4. Onay planı oluştur ──
    cart_total = cart.get("total", 0)
    item_count = len(items)
    applied_coupon = cart.get("appliedCoupon", None)

    # Adım bilgileri
    steps = [
        {"action": "Sepet Doğrulama", "description": f"{item_count} ürün, toplam {cart_total} TL"},
    ]

    if applied_coupon:
        steps.append({"action": "Kupon Uygulama", "description": f"'{applied_coupon}' kuponu uygulanacak"})

    if address:
        address_label = address.get("label", address.get("city", "Kayıtlı Adres"))
        steps.append({"action": "Teslimat Adresi", "description": f"Adres: {address_label}"})
    else:
        steps.append({"action": "Teslimat Adresi", "description": "⚠️ Kayıtlı adresiniz yok, lütfen profil sayfanızdan ekleyin"})

    steps.append({"action": "Ödeme İşlemi", "description": "Kapıda ödeme / Kayıtlı kart"})
    steps.append({"action": "Sipariş Oluşturma", "description": "Sipariş onaylandıktan sonra oluşturulacak"})

    plan = {
        "summary": f"{item_count} ürün için {cart_total} TL tutarında sipariş oluşturulacak.",
        "totalAmount": cart_total,
        "itemCount": item_count,
        "steps": steps,
        "shippingAddressId": address.get("id") if address else None,
        "paymentMethod": "KAPIDA_ODEME",
        "couponCode": applied_coupon,
    }

    plan_json = json.dumps(plan, ensure_ascii=False)

    # Adres yoksa onay oluşturmadan uyar
    if not address:
        return {
            **state,
            "final_response": (
                f"Sepetinizde {item_count} ürün var (toplam {cart_total} TL). "
                "Ancak kayıtlı teslimat adresiniz yok. "
                "Lütfen önce profil sayfanızdan bir adres ekleyin, sonra tekrar deneyin."
            ),
            "agent_type": "checkout_agent",
            "action_type": "INFO",
        }

    # Günlük limit kontrolü
    if preferences:
        today_count = preferences.get("todayTransactionCount", 0)
        daily_limit = preferences.get("dailyTransactionLimit", 10)
        if today_count >= daily_limit:
            return {
                **state,
                "final_response": f"Günlük AI işlem limitinize ulaştınız ({daily_limit}). "
                                  "Lütfen yarın tekrar deneyin veya manuel sipariş verin.",
                "agent_type": "checkout_agent",
                "action_type": "INFO",
            }

    # ── 6. Sessiz Doğrulama (Pre-validation) ──
    validation_state = await pre_validation_agent({**state, "action_data": plan})
    if validation_state.get("error"):
        return {
            **state,
            "final_response": f"Sipariş planı oluşturulurken bir sorun tespit edildi: {validation_state['error']}",
            "agent_type": "checkout_agent",
            "action_type": "INFO",
            "requires_approval": False
        }

    # ── 7. Backend'de onay kaydı oluştur ──
    approval_result = await _create_approval(
        user_id=user_id,
        plan_data=plan_json,
        session_id=session_id,
    )

    if not approval_result:
        return {
            **state,
            "final_response": "Onay planı oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "checkout_agent",
            "action_type": "ERROR",
        }

    approval_token = approval_result.get("approvalToken")

    # ── 8. Frontend'e APPROVAL_REQUIRED kartı gönder ──
    return {
        **state,
        "final_response": (
            f"Sepetinizdeki {item_count} ürün için {cart_total} TL tutarında sipariş "
            f"oluşturmak üzere bir plan hazırladım. Lütfen aşağıdaki detayları inceleyip onaylayın."
        ),
        "agent_type": "checkout_agent",
        "action_type": "APPROVAL_REQUIRED",
        "action_data": {
            "approvalToken": approval_token,
            "planData": plan_json,
            "steps": steps,
            "totalAmount": cart_total,
            "itemCount": item_count,
            "shippingAddress": address.get("label", "Kayıtlı Adres") if address else None,
            "paymentMethod": "Kapıda Ödeme",
        },
        "requires_approval": True,
        "plan_data": plan_json,
        "approval_token": approval_token,
    }
