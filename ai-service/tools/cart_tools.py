"""
tools/cart_tools.py — Spring Boot /api/cart endpoint'lerine HTTP çağrıları.

KRİTİK GÜVENLİK KURALI:
Tüm cart operasyonları user_id'yi X-Authenticated-User-Id header'ı üzerinden geçirir.
Kullanıcı mesajından gelen hiçbir user_id değeri kabul edilmez.
Bu sayede bir kullanıcı başkasının sepetine müdahale edemez.
"""

import httpx
import structlog
from langchain_core.tools import tool
from typing import Optional

from config import settings

logger = structlog.get_logger(__name__)

_http_client = httpx.AsyncClient(timeout=10.0)


def _auth_headers(user_id: str) -> dict:
    """
    Kimlik doğrulamalı istek header'ları.
    user_id Spring Boot'un doğruladığı JWT'den gelir.
    """
    return {
        "X-Internal-Key": settings.spring_boot_internal_key,
        "X-Authenticated-User-Id": str(user_id),
        "Content-Type": "application/json",
    }


@tool
async def get_cart(user_id: str) -> dict:
    """
    Kullanıcının mevcut sepetini getir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si (ZORUNLU)

    Returns:
        Sepet objesi (items, subtotal, total vb.)
    """
    if not user_id:
        return {"error": "Sepeti görüntülemek için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/internal/agent/user/{user_id}/cart-summary"

    try:
        response = await _http_client.get(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        logger.info("cart_get_success", user_id=user_id)
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 401:
            return {"error": "Oturum süresi dolmuş. Lütfen tekrar giriş yapın."}
        logger.error("cart_get_http_error", status=e.response.status_code, user_id=user_id)
        return {"error": f"Sepet alınamadı: {e.response.status_code}"}
    except httpx.RequestError as e:
        logger.error("cart_get_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def add_to_cart(
    user_id: str,
    product_id: int,
    quantity: int = 1,
    variant_id: Optional[int] = None,
) -> dict:
    """
    Sepete ürün ekle.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si (ZORUNLU)
        product_id: Eklenecek ürün ID'si
        quantity: Eklenecek adet (varsayılan: 1)
        variant_id: Ürün varyant ID'si (renk/beden seçimi varsa)

    Returns:
        Güncellenmiş sepet objesi
    """
    if not user_id:
        return {"error": "Sepete eklemek için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/internal/agent/cart/items"
    payload = {
        "productId": product_id,
        "quantity": quantity,
    }
    if variant_id:
        payload["variantId"] = variant_id

    try:
        response = await _http_client.post(
            url,
            json=payload,
            headers=_auth_headers(user_id),
        )
        response.raise_for_status()
        logger.info("cart_add_success", user_id=user_id, product_id=product_id, quantity=quantity)
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 422:
            return {"error": "Stok yetersiz veya ürün mevcut değil."}
        if e.response.status_code == 401:
            return {"error": "Oturum süresi dolmuş. Lütfen tekrar giriş yapın."}
        logger.error("cart_add_http_error", status=e.response.status_code, user_id=user_id)
        return {"error": "Ürün sepete eklenemedi."}
    except httpx.RequestError as e:
        logger.error("cart_add_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def update_cart_item(
    user_id: str,
    item_id: int,
    quantity: int,
) -> dict:
    """
    Sepetteki ürün miktarını güncelle.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si
        item_id: Güncellenecek sepet öğesi ID'si
        quantity: Yeni miktar

    Returns:
        Güncellenmiş sepet objesi
    """
    if not user_id:
        return {"error": "Bu işlem için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/internal/agent/cart/items/{item_id}"

    try:
        response = await _http_client.put(
            url,
            json={"quantity": quantity},
            headers=_auth_headers(user_id),
        )
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 403:
            return {"error": "Bu sepet öğesi size ait değil."}
        logger.error("cart_update_http_error", status=e.response.status_code)
        return {"error": "Sepet güncellenemedi."}
    except httpx.RequestError as e:
        logger.error("cart_update_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def remove_from_cart(user_id: str, item_id: int) -> dict:
    """
    Sepetten ürün çıkar.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si
        item_id: Çıkarılacak sepet öğesi ID'si

    Returns:
        Güncellenmiş sepet objesi
    """
    if not user_id:
        return {"error": "Bu işlem için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/internal/agent/cart/items/{item_id}"

    try:
        response = await _http_client.delete(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 403:
            return {"error": "Bu sepet öğesi size ait değil."}
        logger.error("cart_remove_http_error", status=e.response.status_code)
        return {"error": "Ürün sepetten çıkarılamadı."}
    except httpx.RequestError as e:
        logger.error("cart_remove_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def clear_cart(user_id: str) -> dict:
    """
    Sepeti tamamen temizle.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Boş sepet objesi
    """
    if not user_id:
        return {"error": "Bu işlem için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/internal/agent/cart"

    try:
        response = await _http_client.delete(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        logger.info("cart_clear_success", user_id=user_id)
        return {"message": "Sepetiniz temizlendi.", "cleared": True}
    except httpx.HTTPStatusError as e:
        logger.error("cart_clear_http_error", status=e.response.status_code)
        return {"error": "Sepet temizlenemedi."}
    except httpx.RequestError as e:
        logger.error("cart_clear_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}
