"""
agents/pre_validation_agent.py — İşlem Öncesi Doğrulama Agent'ı.

Bir Agent (örn: CheckoutAgent) plan hazırlamadan önce veya planı sunmadan önce,
bu agent arka planda "sessiz" bir doğrulama yapar:
  - Ürünlerin stokları yeterli mi?
  - Fiyatlar değişti mi?
  - Kullanıcının günlük işlem limiti doldu mu?
  - Seçili adres hala geçerli mi?

Bu agent'ın görevi 'final_response' üretmek değil, state'teki validasyon bayraklarını set etmektir.
"""

import structlog
import httpx
from config import settings
from graph.state import AgentState

logger = structlog.get_logger(__name__)

INTERNAL_API = f"{settings.spring_boot_base_url}/internal/agent"

async def pre_validation_agent(state: AgentState) -> dict:
    """
    Sessiz doğrulama yapar.
    Hata bulursa state.error set eder ve requires_approval'ı False yapar.
    """
    user_id = state.get("user_id")
    if not user_id:
        return {"error": "Giriş yapılmamış"}

    logger.info("pre_validation_started", user_id=user_id)
    
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            # 1. Stok ve Sepet Kontrolü (Backend endpoint'e istek atılır)
            # Backend tarafında QuickCheckoutService içindeki validateCheckout mantığını proxy eder.
            resp = await client.post(
                f"{settings.spring_boot_base_url}/agent/quick-checkout/validate",
                json={
                    "shippingAddressId": state.get("action_data", {}).get("shippingAddressId"),
                    "couponCode": state.get("action_data", {}).get("couponCode")
                },
                headers={
                    "X-Authenticated-User-Id": str(user_id),
                    "X-Internal-Key": settings.spring_boot_internal_key
                }
            )
            
            if resp.status_code != 200:
                logger.warning("pre_validation_failed_http", status=resp.status_code)
                return {"error": "Doğrulama servisi şu an kullanılamıyor."}
            
            val_data = resp.json()
            if not val_data.get("valid", False):
                issues = val_data.get("issues", ["Bilinmeyen doğrulama hatası"])
                logger.info("pre_validation_issues_found", issues=issues)
                return {
                    "error": " / ".join(issues),
                    "requires_approval": False
                }
                
            logger.info("pre_validation_success", user_id=user_id)
            return {"error": None}

    except Exception as e:
        logger.error("pre_validation_exception", error=str(e))
        return {"error": "Bağlantı hatası sebebiyle doğrulama yapılamadı."}
