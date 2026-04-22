"""
tools/user_tools.py — Kullanıcı profil bilgileri için Spring Boot API araçları.

Kullanıcının profil bilgilerini, adreslerini ve tercihlerini getirir.
Tüm çağrılar user_id ile scope edilir.
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
async def get_user_profile(user_id: str) -> dict:
    """
    Kullanıcının profil bilgilerini getir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Kullanıcı profil objesi (ad, soyad, email, rol, vb.)
    """
    if not user_id:
        return {"error": "Profil bilgisi için giriş yapmanız gerekiyor."}

    url = f"{settings.spring_boot_base_url}/users/me"

    try:
        response = await _http_client.get(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        logger.info("user_profile_fetch_success", user_id=user_id)
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 401:
            return {"error": "Oturum süresi dolmuş. Lütfen tekrar giriş yapın."}
        logger.error("user_profile_http_error", status=e.response.status_code)
        return {"error": "Profil bilgisi alınamadı."}
    except httpx.RequestError as e:
        logger.error("user_profile_connection_error", error=str(e))
        return {"error": "Sunucuya bağlanılamadı."}


@tool
async def get_user_addresses(user_id: str) -> list:
    """
    Kullanıcının kayıtlı adreslerini getir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Adres listesi
    """
    if not user_id:
        return []

    url = f"{settings.spring_boot_base_url}/users/me/addresses"

    try:
        response = await _http_client.get(url, headers=_auth_headers(user_id))
        response.raise_for_status()
        return response.json()
    except Exception as e:
        logger.error("user_addresses_error", error=str(e))
        return []
