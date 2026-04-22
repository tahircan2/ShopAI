"""
models/schemas.py — FastAPI endpoint'leri için Pydantic şemaları.

ChatRequest: Angular → Spring Boot → Python proxy üzerinden gelir.
userId ASLA request body'de gelmez; X-Authenticated-User-Id header'ından alınır.

KRİTİK: ChatResponse camelCase alias kullanır.
Python field'ları snake_case kalır ama JSON serialize'da camelCase döner:
  agent_type → agentType
  action_type → actionType
  injection_detected → injectionDetected
Bu sayede Java (AiService) ve Angular (AiChatService) doğrudan okuyabilir.
"""

from pydantic import BaseModel, ConfigDict, Field, field_validator
from pydantic.alias_generators import to_camel
from typing import Optional, Any
from enum import Enum


class AgentActionType(str, Enum):
    PRODUCT_LIST = "PRODUCT_LIST"
    CART_UPDATED = "CART_UPDATED"
    NAVIGATE = "NAVIGATE"
    INFO = "INFO"
    ORDER_INFO = "ORDER_INFO"
    ERROR = "ERROR"
    # Agentic UI Control
    APPROVAL_REQUIRED = "APPROVAL_REQUIRED"   # Frontend'de onay kartı göster
    STEP_PROGRESS = "STEP_PROGRESS"           # Frontend'de progress stepper göster
    CHECKOUT_COMPLETE = "CHECKOUT_COMPLETE"   # Checkout tamamlandı bildirimi


class IntentType(str, Enum):
    PRODUCT_FILTER = "PRODUCT_FILTER"
    PRODUCT_DETAIL = "PRODUCT_DETAIL"
    CART_ACTION = "CART_ACTION"
    RECOMMENDATION = "RECOMMENDATION"
    ORDER_QUERY = "ORDER_QUERY"
    FAQ = "FAQ"
    GENERAL = "GENERAL"
    CHECKOUT = "CHECKOUT"
    NAVIGATE = "NAVIGATE"
    USER_PROFILE = "USER_PROFILE"


class ChatRequest(BaseModel):
    """
    Kullanıcıdan gelen chat isteği.
    userId bu modelde YOK — Spring Boot'un X-Authenticated-User-Id header'ından alınır.
    Gelen JSON camelCase olabilir (sessionId) — populate_by_name ile snake_case de kabul edilir.
    """

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

    session_id: str = Field(..., description="Frontend session UUID", min_length=1, max_length=100)
    message: str = Field(..., description="Kullanıcı mesajı", min_length=1, max_length=500)
    conversation_history: list[dict] = Field(default_factory=list, description="Geçmiş mesajlar listesi")

    @field_validator("message")
    @classmethod
    def sanitize_message(cls, v: str) -> str:
        """Temel sanitizasyon — boşlukları temizle."""
        return v.strip()

    @field_validator("session_id")
    @classmethod
    def validate_session_id(cls, v: str) -> str:
        """Session ID alfanümerik ve tire içerebilir."""
        import re
        if not re.match(r"^[a-zA-Z0-9\-_]+$", v):
            raise ValueError("Geçersiz session_id formatı")
        return v


class ChatResponse(BaseModel):
    """
    Python AI servisinden dönen yanıt.

    camelCase alias ile serialize edilir:
      agent_type    → agentType
      action_type   → actionType
      action_data   → actionData
      injection_detected → injectionDetected
      session_id    → sessionId
    """

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

    message: str = Field(..., description="Kullanıcıya gösterilecek yanıt")
    agent_type: Optional[str] = Field(None, description="Yanıtı üreten agent adı")
    action_type: Optional[AgentActionType] = Field(None, description="Frontend aksiyonu")
    action_data: Optional[Any] = Field(None, description="Aksiyon için JSON data")
    injection_detected: bool = Field(default=False, description="Prompt injection tespit edildi mi")
    session_id: str = Field(..., description="İstek session ID'si")
    intent: Optional[str] = Field(None, description="Tespit edilen niyet")
    # Agentic UI Control fields
    requires_approval: bool = Field(default=False, description="Kullanıcı onayı gerekiyor mu")
    approval_token: Optional[str] = Field(None, description="Backend onay token'ı")
    plan_data: Optional[str] = Field(None, description="İşlem planı JSON")
    transaction_id: Optional[int] = Field(None, description="AgentTransaction ID")


class HealthResponse(BaseModel):
    """Health check yanıtı."""

    status: str = "ok"
    service: str = "shopai-ai-service"
    version: str = "3.0.0"


class ProductFilter(BaseModel):
    """Filter Agent'ın ürettiği ürün filtresi — Spring Boot query params ile eşleşir."""

    category: Optional[str] = None
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    colors: Optional[list[str]] = None
    sizes: Optional[list[str]] = None
    brand: Optional[str] = None
    rating: Optional[float] = None
    sort_by: Optional[str] = None
    sort_dir: Optional[str] = None
    page: int = 0
    size: int = 10
    q: Optional[str] = None


class CartActionRequest(BaseModel):
    """Cart Agent'ın sepet işlemi için kullandığı model."""

    product_id: Optional[int] = None
    variant_id: Optional[int] = None
    quantity: int = 1
    action: str  # ADD, REMOVE, CLEAR, GET


class InternalChatPayload(BaseModel):
    """
    Spring Boot'tan Python'a iletilen tam payload.
    user_id Spring Boot'un doğruladığı JWT'den extract edilir.
    """

    session_id: str
    message: str
    user_id: Optional[str] = None  # JWT'den extract edilen — kullanıcı girdisinden değil
    conversation_history: Optional[list[dict]] = Field(default_factory=list)
