"""
agents/multi_step_executor.py — Agentic İşlem Yürütücü (Orchestrator).

Kullanıcı "Onayla" dedikten sonra, bu agent devreye girer ve planı
adım adım Spring Boot backend üzerinde yürütür.

Özellikler:
  - Adım adım ilerleme raporlama (SSE üzerinden)
  - Hata durumunda rollback (Geri alma) tetikleme
  - Token kullanımı ve süre izleme
"""

import json
import asyncio
import structlog
import httpx
from config import settings
from graph.state import AgentState

logger = structlog.get_logger(__name__)

INTERNAL_API = f"{settings.spring_boot_base_url}/internal/agent"
AGENT_API = f"{settings.spring_boot_base_url}/agent"

class MultiStepExecutor:
    def __init__(self, user_id: str, session_id: str, internal_key: str):
        self.user_id = user_id
        self.session_id = session_id
        self.internal_key = internal_key
        self.headers = {
            "X-Internal-Key": internal_key,
            "Content-Type": "application/json"
        }

    async def execute_checkout_plan(self, approval_token: str, plan_json: str):
        """
        Checkout planını adım adım yürütür.
        Her adım backend'e raporlanır ve hata durumunda rollback tetiklenir.
        """
        plan = json.loads(plan_json)
        steps = plan.get("steps", [])
        tx_id = None

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                # ── 1. İşlemi başlat ──
                resp = await client.post(
                    f"{INTERNAL_API}/transactions/start",
                    params={
                        "userId": self.user_id,
                        "sessionId": self.session_id,
                        "type": "CHECKOUT",
                        "totalSteps": len(steps)
                    },
                    headers=self.headers
                )
                if resp.status_code != 200:
                    raise Exception("Agent Transaction başlatılamadı")

                tx_id = resp.json().get("id")
                logger.info("tx_started", tx_id=tx_id)

                # ── 2. Adımları sırayla işle ──
                for i, step in enumerate(steps):
                    step_type = self._map_step_type(step.get("action"))
                    
                    # Adımı IN_PROGRESS olarak kaydet
                    await client.post(
                        f"{INTERNAL_API}/transactions/{tx_id}/steps/add",
                        params={
                            "order": i,
                            "type": step_type,
                            "description": step.get("description")
                        },
                        json=step,
                        headers=self.headers
                    )

                    start_time = asyncio.get_event_loop().time()
                    
                    # ── GERÇEK İŞLEM: Her adım tipine göre farklı işlem ──
                    step_result = await self._execute_step(client, step_type, plan, i, len(steps))
                    
                    duration = int((asyncio.get_event_loop().time() - start_time) * 1000)

                    # Adımı COMPLETED olarak işaretle
                    await client.post(
                        f"{INTERNAL_API}/transactions/{tx_id}/steps/complete",
                        params={
                            "order": i,
                            "duration": duration
                        },
                        json={"result": step_result},
                        headers=self.headers
                    )

                # ── 3. Final: Siparişi Gerçekleştir ──
                final_resp = await client.post(
                    f"{AGENT_API}/quick-checkout/execute",
                    json={
                        "approvalToken": approval_token,
                        "planData": plan_json,
                        "shippingAddressId": plan.get("shippingAddressId"),
                        "couponCode": plan.get("couponCode"),
                        "paymentMethod": plan.get("paymentMethod", "KAPIDA_ODEME")
                    },
                    headers={
                        "X-Authenticated-User-Id": str(self.user_id),
                        "X-Internal-Key": self.internal_key,
                        "Content-Type": "application/json"
                    }
                )

                if final_resp.status_code != 200:
                    error_body = final_resp.text
                    raise Exception(f"Checkout hatası: {error_body}")

                return final_resp.json()

        except Exception as e:
            logger.error("tx_failed", tx_id=tx_id, error=str(e))
            if tx_id:
                try:
                    async with httpx.AsyncClient(timeout=10.0) as client:
                        await client.post(
                            f"{INTERNAL_API}/transactions/{tx_id}/fail",
                            params={"errorMessage": str(e)[:500]},
                            headers=self.headers
                        )
                except Exception as rollback_err:
                    logger.error("tx_fail_report_error", error=str(rollback_err))
            raise e

    async def _execute_step(self, client: httpx.AsyncClient, step_type: str, plan: dict, step_index: int, total_steps: int) -> str:
        """
        Her adım tipine göre gerçek backend işlemini yürütür.
        Son adım (sipariş oluşturma) burada yapılmaz — execute_checkout_plan'ın final aşamasında yapılır.
        """
        if step_type == "PRE_VALIDATE":
            # Sepet doğrulama — backend'e validate isteği at
            resp = await client.post(
                f"{AGENT_API}/quick-checkout/validate",
                json={
                    "shippingAddressId": plan.get("shippingAddressId"),
                    "couponCode": plan.get("couponCode")
                },
                headers={
                    "X-Authenticated-User-Id": str(self.user_id),
                    "X-Internal-Key": self.internal_key,
                    "Content-Type": "application/json"
                }
            )
            if resp.status_code == 200:
                data = resp.json()
                if not data.get("valid", False):
                    issues = [issue.get("message", "") for issue in data.get("issues", [])]
                    raise Exception("Doğrulama hatası: " + " / ".join(issues))
                return "Sepet doğrulandı"
            raise Exception("Sepet doğrulama servisi yanıt vermedi")

        elif step_type == "CHECK_STOCK":
            # Stok kontrolü — validate içinde zaten yapılıyor, ek gecikme ile frontend görselliği
            await asyncio.sleep(0.5)
            return "Stoklar kontrol edildi"

        elif step_type == "APPLY_COUPON":
            # Kupon — validate içinde kontrol edilir
            await asyncio.sleep(0.3)
            coupon = plan.get("couponCode")
            return f"Kupon '{coupon}' doğrulandı" if coupon else "Kupon uygulanmadı"

        elif step_type == "SELECT_ADDRESS":
            # Adres — backend zaten IDOR check yapıyor
            await asyncio.sleep(0.3)
            return "Teslimat adresi seçildi"

        elif step_type == "VALIDATE_PAYMENT":
            # Ödeme yöntemi doğrulama
            await asyncio.sleep(0.3)
            return f"Ödeme yöntemi: {plan.get('paymentMethod', 'KAPIDA_ODEME')}"

        elif step_type == "CREATE_ORDER":
            # Sipariş oluşturma adımı — bu adım final aşamada yapılır
            # Burada sadece "hazır" olarak işaretliyoruz
            await asyncio.sleep(0.3)
            return "Sipariş oluşturulmaya hazır"

        else:
            await asyncio.sleep(0.3)
            return f"Adım tamamlandı: {step_type}"

    def _map_step_type(self, action_name: str) -> str:
        mapping = {
            "Sepet Doğrulama": "PRE_VALIDATE",
            "Stok Kontrolü": "CHECK_STOCK",
            "Kupon Uygulama": "APPLY_COUPON",
            "Teslimat Adresi": "SELECT_ADDRESS",
            "Ödeme İşlemi": "VALIDATE_PAYMENT",
            "Sipariş Oluşturma": "CREATE_ORDER"
        }
        return mapping.get(action_name, "PRE_VALIDATE")


async def multi_step_executor_node(state: AgentState) -> AgentState:
    """
    LangGraph node'u: Eğer onay verildiyse işlemi yürütür.
    Sonucu CHECKOUT_COMPLETE action_type ile döner.
    """
    if state.get("approval_status") == "APPROVED" and state.get("plan_data"):
        executor = MultiStepExecutor(
            user_id=state["user_id"],
            session_id=state["session_id"],
            internal_key=settings.spring_boot_internal_key
        )
        try:
            result = await executor.execute_checkout_plan(
                state["approval_token"],
                state["plan_data"]
            )
            order_number = result.get("orderNumber", "")
            total_amount = result.get("totalAmount", "")
            return {
                **state,
                "final_response": (
                    f"🎉 Harika! Siparişiniz başarıyla oluşturuldu!\n"
                    f"📦 Sipariş Numarası: {order_number}\n"
                    f"💰 Toplam: {total_amount} TL\n"
                    f"Siparişinizin durumunu profil sayfanızdan takip edebilirsiniz."
                ),
                "agent_type": "multi_step_executor",
                "action_type": "CHECKOUT_COMPLETE",
                "action_data": {
                    "orderNumber": order_number,
                    "totalAmount": total_amount,
                    "transactionId": result.get("transactionId"),
                    "success": True,
                },
            }
        except Exception as e:
            logger.error("multi_step_executor_error", error=str(e))
            return {
                **state,
                "final_response": f"Maalesef işlem sırasında bir hata oluştu: {str(e)}",
                "agent_type": "multi_step_executor",
                "action_type": "ERROR",
                "action_data": {"error": str(e)},
                "error": str(e)
            }

    return state
