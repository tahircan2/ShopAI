"""
tools/approval_tools.py — Onay yönetimi araçları.
"""

import httpx
import structlog
from config import settings

logger = structlog.get_logger(__name__)

INTERNAL_API = f"{settings.spring_boot_base_url}/internal/agent"

async def get_latest_approval(user_id: str) -> dict | None:
    """
    Kullanıcının son aktif onay kaydını (PENDING veya APPROVED) getirir.
    Bu araç, kullanıcının "onayladım" dediği ancak state'in kaybolduğu durumlarda
    bağlamı geri kazanmak için kullanılır.
    """
    if not user_id:
        return None

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{INTERNAL_API}/approvals/latest",
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id)
                }
            )
            if resp.status_code == 200:
                return resp.json()
            return None
    except Exception as e:
        logger.error("get_latest_approval_error", error=str(e))
        return None

async def approve_transaction(user_id: str, token: str) -> bool:
    """İşlem onayını backend'de APPROVED olarak işaretler."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                f"{INTERNAL_API}/approvals/{token}/approve",
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id)
                }
            )
            return resp.status_code == 200
    except Exception as e:
        logger.error("approve_transaction_error", error=str(e))
        return False

async def reject_transaction(user_id: str, token: str) -> bool:
    """İşlem onayını backend'de REJECTED olarak işaretler."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                f"{INTERNAL_API}/approvals/{token}/reject",
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id)
                }
            )
            return resp.status_code == 200
    except Exception as e:
        logger.error("reject_transaction_error", error=str(e))
        return False
