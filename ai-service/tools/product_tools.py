"""
tools/product_tools.py — Spring Boot /api/products endpoint'lerine HTTP çağrıları.

Tüm tool fonksiyonları LangChain @tool decorator'ı ile tanımlanır.
user_id parametresi her tool'a geçirilir — kullanıcı verisi izolasyonu için zorunludur.
"""

import httpx
import structlog
from langchain_core.tools import tool
from typing import Optional

from config import settings

logger = structlog.get_logger(__name__)

# Internal HTTP client — her istekte yeniden oluşturmamak için module-level
_http_client = httpx.AsyncClient(timeout=10.0)


def _internal_headers(user_id: Optional[str] = None) -> dict:
    """Spring Boot iç servis header'larını döner."""
    headers = {
        "X-Internal-Key": settings.spring_boot_internal_key,
        "Content-Type": "application/json",
    }
    if user_id:
        headers["X-Authenticated-User-Id"] = str(user_id)
    return headers


@tool
async def search_products(
    query: str,
    user_id: Optional[str] = None,
    page: int = 0,
    size: int = 10,
) -> dict:
    """
    Ürün arama — kullanıcının doğal dil sorgusunu full-text search'e dönüştür.

    Args:
        query: Arama terimi (ör. 'nike ayakkabı', 'kırmızı elbise')
        user_id: JWT'den extract edilen kullanıcı ID'si (izolasyon için)
        page: Sayfa numarası
        size: Sayfa başı kayıt sayısı

    Returns:
        Spring Boot ProductPage response (content, totalElements, totalPages)
    """
    url = f"{settings.spring_boot_base_url}/products/search"
    params = {"q": query, "page": page, "size": size}

    try:
        response = await _http_client.get(
            url,
            params=params,
            headers=_internal_headers(user_id),
        )
        response.raise_for_status()
        logger.info("product_search_success", query=query, user_id=user_id)
        return response.json()
    except httpx.HTTPStatusError as e:
        logger.error("product_search_http_error", status=e.response.status_code, query=query)
        return {"error": f"Ürün arama başarısız: {e.response.status_code}", "content": []}
    except httpx.RequestError as e:
        logger.error("product_search_connection_error", error=str(e))
        return {"error": "Spring Boot'a bağlanılamadı", "content": []}


@tool
async def filter_products(
    category: Optional[str] = None,
    min_price: Optional[float] = None,
    max_price: Optional[float] = None,
    colors: Optional[list[str]] = None,
    sizes: Optional[list[str]] = None,
    brand: Optional[str] = None,
    rating: Optional[float] = None,
    sort_by: Optional[str] = None,
    sort_dir: Optional[str] = "asc",
    page: int = 0,
    size: int = 10,
    user_id: Optional[str] = None,
) -> dict:
    """
    Ürün filtreleme — çoklu parametre ile filtrelenmiş ürün listesi döner.

    Args:
        category: Kategori adı veya slug
        min_price: Minimum fiyat (TL)
        max_price: Maksimum fiyat (TL)
        colors: Renk listesi (ör. ['Kırmızı', 'Mavi'])
        sizes: Beden listesi (ör. ['S', 'M', 'L'])
        brand: Marka adı
        rating: Minimum puan (1-5)
        sort_by: Sıralama alanı (price, rating, ratingCount, createdAt)
        sort_dir: Sıralama yönü (asc, desc)
        page: Sayfa numarası
        size: Sayfa başı kayıt sayısı
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Filtrelenmiş ProductPage response
    """
    url = f"{settings.spring_boot_base_url}/products"
    params: dict = {"page": page, "size": size}

    if category:
        # Gelen kategori ismini slug formatına dönüştür
        slug = category.lower().replace("ş", "s").replace("ı", "i").replace("ğ", "g").replace("ü", "u").replace("ö", "o").replace("ç", "c").replace(" ", "-")
        params["categorySlug"] = slug
    if min_price is not None:
        params["minPrice"] = min_price
    if max_price is not None:
        params["maxPrice"] = max_price
    if colors:
        params["colors"] = ",".join(colors)
    if sizes:
        params["sizes"] = ",".join(sizes)
    if brand:
        params["brand"] = brand
    if rating is not None:
        params["minRating"] = rating
    if sort_by:
        params["sortBy"] = sort_by
        params["sortDir"] = sort_dir or "asc"

    try:
        response = await _http_client.get(
            url,
            params=params,
            headers=_internal_headers(user_id),
        )
        response.raise_for_status()
        logger.info("product_filter_success", params=params, user_id=user_id)
        return response.json()
    except httpx.HTTPStatusError as e:
        logger.error("product_filter_http_error", status=e.response.status_code)
        return {"error": f"Ürün filtreleme başarısız: {e.response.status_code}", "content": []}
    except httpx.RequestError as e:
        logger.error("product_filter_connection_error", error=str(e))
        return {"error": "Spring Boot'a bağlanılamadı", "content": []}


@tool
async def get_product_detail(
    product_id: int,
    user_id: Optional[str] = None,
) -> dict:
    """
    Tek ürün detayını getir.

    Args:
        product_id: Ürün ID'si
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Ürün detay objesi (varyantlar ve görseller dahil)
    """
    url = f"{settings.spring_boot_base_url}/products/{product_id}"

    try:
        response = await _http_client.get(url, headers=_internal_headers(user_id))
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as e:
        logger.error("product_detail_http_error", product_id=product_id, status=e.response.status_code)
        return {"error": f"Ürün bulunamadı: {product_id}"}
    except httpx.RequestError as e:
        logger.error("product_detail_connection_error", error=str(e))
        return {"error": "Spring Boot'a bağlanılamadı"}


@tool
async def get_featured_products(user_id: Optional[str] = None) -> dict:
    """
    Öne çıkan ürünleri getir.

    Args:
        user_id: JWT'den extract edilen kullanıcı ID'si

    Returns:
        Öne çıkan ürün listesi
    """
    url = f"{settings.spring_boot_base_url}/products/featured"

    try:
        response = await _http_client.get(url, headers=_internal_headers(user_id))
        response.raise_for_status()
        return {"content": response.json(), "featured": True}
    except httpx.HTTPStatusError as e:
        logger.error("featured_products_http_error", status=e.response.status_code)
        return {"error": "Öne çıkan ürünler alınamadı", "content": []}
    except httpx.RequestError as e:
        logger.error("featured_products_connection_error", error=str(e))
        return {"error": "Spring Boot'a bağlanılamadı", "content": []}


@tool
async def get_categories(user_id: Optional[str] = None) -> list:
    """
    Tüm kategorileri ağaç yapısında getir.

    Returns:
        Kategori listesi (hiyerarşik)
    """
    url = f"{settings.spring_boot_base_url}/categories"

    try:
        response = await _http_client.get(url, headers=_internal_headers(user_id))
        response.raise_for_status()
        return response.json()
    except httpx.RequestError as e:
        logger.error("categories_connection_error", error=str(e))
        return []
