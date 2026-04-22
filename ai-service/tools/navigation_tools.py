"""
tools/navigation_tools.py — UI navigasyon aksiyonları için tool'lar.
Bu tool'lar doğrudan DB çağrısı yapmaz, sadece LangGraph state'ine 
NAVIGATE aksiyonu eklenmesi için gerekli parametreleri hazırlar.
"""

from langchain_core.tools import tool
from typing import Optional, Dict, Any
import structlog

logger = structlog.get_logger(__name__)

@tool
def navigate_to_page(
    page_name: str,
    params: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """
    Kullanıcıyı belirli bir sayfaya yönlendirir.
    
    Args:
        page_name: Sayfa adı (HOME, SHOP, CART, CHECKOUT, PROFILE, ORDERS, CONTACT)
        params: URL query parametreleri veya path değişkenleri (ör. {'id': 123}, {'category': 'elektronik'})
        
    Returns:
        Navigation metadata
    """
    valid_pages = {
        "HOME": "/",
        "SHOP": "/shop",
        "CART": "/cart",
        "CHECKOUT": "/checkout",
        "PROFILE": "/profile",
        "ORDERS": "/profile/orders",
        "CONTACT": "/contact"
    }
    
    path = valid_pages.get(page_name.upper(), "/shop")
    
    logger.info("navigation_intent_created", page=page_name, path=path, params=params)
    
    return {
        "action": "NAVIGATE",
        "path": path,
        "params": params,
        "page_name": page_name.upper()
    }

@tool
def navigate_to_product(
    product_id: int,
    product_slug: Optional[str] = None
) -> Dict[str, Any]:
    """
    Kullanıcıyı belirli bir ürünün detay sayfasına yönlendirir.
    
    Args:
        product_id: Ürün ID'si
        product_slug: Ürün slug'ı (URL için tercih edilir)
    """
    path = f"/product/{product_slug or product_id}"
    
    return {
        "action": "NAVIGATE",
        "path": path,
        "product_id": product_id
    }

@tool
def navigate_to_category(
    category_slug: str
) -> Dict[str, Any]:
    """
    Kullanıcıyı belirli bir kategori filtresiyle shop sayfasına yönlendirir.
    
    Args:
        category_slug: Kategori slug'ı
    """
    return {
        "action": "NAVIGATE",
        "path": "/shop",
        "params": {"category": category_slug}
    }
