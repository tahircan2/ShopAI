"""
tools/checkout_tools.py — Checkout işlemleri için Spring Boot API araçları.

Bu araçlar, checkout_agent ve multi_step_executor tarafından kullanılır.
Tüm API çağrıları internal key ile korunur.
"""

import structlog
import httpx
from config import settings

logger = structlog.get_logger(__name__)

INTERNAL_API = f"{settings.spring_boot_base_url}/api/internal/agent"
AGENT_API = f"{settings.spring_boot_base_url}/api/agent"


async def get_user_default_address(user_id: str) -> dict | None:
    """Kullanıcının varsayılan teslimat adresini getirir."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{INTERNAL_API}/user/{user_id}/default-address",
                headers={"X-Internal-Key": settings.spring_boot_internal_key},
            )
            if resp.status_code == 200:
                return resp.json()
            return None
    except Exception as e:
        logger.error("get_address_error", error=str(e))
        return None


async def get_user_preferences(user_id: str) -> dict | None:
    """Kullanıcının AI tercihlerini getirir."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{INTERNAL_API}/user/{user_id}/preferences",
                headers={"X-Internal-Key": settings.spring_boot_internal_key},
            )
            if resp.status_code == 200:
                return resp.json()
            return None
    except Exception as e:
        logger.error("get_preferences_error", error=str(e))
        return None


async def get_cart_summary(user_id: str) -> dict | None:
    """Kullanıcının sepet özetini getirir."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{INTERNAL_API}/user/{user_id}/cart-summary",
                headers={"X-Internal-Key": settings.spring_boot_internal_key},
            )
            if resp.status_code == 200:
                return resp.json()
            return None
    except Exception as e:
        logger.error("get_cart_summary_error", error=str(e))
        return None


async def get_applicable_coupons(user_id: str, jwt_cookie: str | None = None) -> list:
    """Sepet için geçerli kuponları getirir."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                f"{AGENT_API}/coupons/applicable",
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id),
                },
            )
            if resp.status_code == 200:
                return resp.json()
            return []
    except Exception as e:
        logger.error("get_coupons_error", error=str(e))
        return []


async def validate_checkout(user_id: str, request_data: dict) -> dict | None:
    """Checkout pre-validation — stok, adres, kupon kontrolü."""
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                f"{AGENT_API}/quick-checkout/validate",
                json=request_data,
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id),
                },
            )
            if resp.status_code == 200:
                return resp.json()
            logger.warning("validate_checkout_error", status=resp.status_code)
            return None
    except Exception as e:
        logger.error("validate_checkout_exception", error=str(e))
        return None


async def execute_checkout(user_id: str, request_data: dict) -> dict | None:
    """Checkout execution — sipariş oluşturma."""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                f"{AGENT_API}/quick-checkout/execute",
                json=request_data,
                headers={
                    "X-Internal-Key": settings.spring_boot_internal_key,
                    "X-Authenticated-User-Id": str(user_id),
                },
            )
            if resp.status_code == 200:
                return resp.json()
            logger.warning("execute_checkout_error", status=resp.status_code, body=resp.text)
            return None
    except Exception as e:
        logger.error("execute_checkout_exception", error=str(e))
        return None
