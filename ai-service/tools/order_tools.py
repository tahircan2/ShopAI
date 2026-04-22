"""
tools/order_tools.py — Spring Boot /api/orders endpoint'lerine HTTP çağrıları.

Tüm sipariş sorguları kullanıcı bazlı scope edilir.
Bir kullanıcı başka kullanıcının siparişini sorgulayamaz — ownership check Spring Boot'ta yapılır.
"""

import httpx
import structlog
from langchain_core.tools import tool
from typing import Optional

from config import settings

logger = structlog.get_logger(__name__)

_http_client = httpx.AsyncClient(timeout=10.0)


def _auth_headers(user_id: str) -> dict:
    return {
        "X-Internal-Key": settings.spring_boot_internal_key,
        "X-Authenticated-User-Id": str(user_id),
        "Content-Type": "application/json",
    }


@tool
async def get_user_orders(
    user_id: str,
    user_role: str = "",
    page: int = 0,
    size: int = 5,
) -> dict:
    """
    Kullanıcının sipariş geçmişini getir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si (ZORUNLU)
        page: Sayfa numarası
        size: Sayfa başı kayıt sayısı

    Returns:
        Kullanıcıya ait sipariş listesi (sayfalı)
    """
    if not user_id:
        return {"error": "Sipariş geçmişi için giriş yapmanız gerekiyor."}

    if user_role == "ROLE_ADMIN":
        url = f"{settings.spring_boot_base_url}/orders/admin"
    else:
        url = f"{settings.spring_boot_base_url}/users/me/orders"

    params = {"page": page, "size": size}

    try:
        response = await _http_client.get(
            url,
            params=params,
            headers=_auth_headers(user_id),
        )
        response.raise_for_status()
        logger.info("orders_fetch_success", user_id=user_id)
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 401:
            return {"error": "Oturum süresi dolmuş. Lütfen tekrar giriş yapın."}
        logger.error("orders_fetch_http_error", status=e.response.status_code, user_id=user_id)
        return {"error": "Siparişler alınamadı."}
    except httpx.RequestError as e:
        logger.error("orders_fetch_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def get_order_detail(
    user_id: str,
    order_number: str,
    user_role: str = "",
) -> dict:
    """
    Belirli bir siparişin detayını getir.

    Spring Boot ownership check uygular — kullanıcı sadece kendi siparişini görebilir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si
        order_number: Sipariş numarası (ör. ORD-20240101-XXXX)

    Returns:
        Sipariş detay objesi veya hata mesajı
    """
    if not user_id:
        return {"error": "Sipariş detayı için giriş yapmanız gerekiyor."}

    if user_role == "ROLE_ADMIN":
        # Adminler tüm siparişlerin detayına erişebilir. OrderController/OrderService backend tarafında desteklemiyorsa 
        # şimdilik /orders/{orderNumber} çağrılır, backend güncellenecektir.
        url = f"{settings.spring_boot_base_url}/orders/{order_number}?isAdmin=true"
    else:
        url = f"{settings.spring_boot_base_url}/orders/{order_number}"

    try:
        response = await _http_client.get(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 403:
            return {"error": "Bu siparişi görüntüleme yetkiniz yok."}
        if e.response.status_code == 404:
            return {"error": f"'{order_number}' numaralı sipariş bulunamadı."}
        logger.error("order_detail_http_error", status=e.response.status_code, order_number=order_number)
        return {"error": "Sipariş detayı alınamadı."}
    except httpx.RequestError as e:
        logger.error("order_detail_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def get_latest_order(user_id: str, user_role: str = "") -> dict:
    """
    Kullanıcının en son siparişini getir (sipariş durumu sorgulaması için kullanışlı).

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        En son sipariş objesi
    """
    if not user_id:
        return {"error": "Sipariş bilgisi için giriş yapmanız gerekiyor."}

    result = await get_user_orders.ainvoke({"user_id": user_id, "user_role": user_role, "page": 0, "size": 1})

    if "error" in result:
        return result

    content = result.get("content", [])
    if not content:
        return {"message": "Henüz hiç sipariş vermediniz."}

    return content[0]
