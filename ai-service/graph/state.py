"""
graph/state.py — LangGraph StateGraph için AgentState tanımı.

AgentState, graph boyunca taşınan tüm durumu kapsar.
user_id YALNIZCA Spring Boot'tan gelen X-Authenticated-User-Id header'ından alınır.
Kullanıcı mesajından veya herhangi bir user input'tan alınmaz.
"""

from typing import Optional, Any, TypedDict
from langchain_core.messages import BaseMessage


class AgentState(TypedDict):
    """
    LangGraph StateGraph için merkezi state tanımı.
    Tüm node'lar bu state'i okur ve günceller.
    """

    # Konuşma geçmişi (LangChain mesaj formatı)
    messages: list[BaseMessage]

    # Kullanıcı kimliği — JWT doğrulaması sonrası Spring Boot tarafından iletilir.
    # ASLA kullanıcı girdisinden alınmaz. None = anonim kullanıcı.
    user_id: Optional[str]

    # Kullanıcı rolü — JWT'den alınır (ROLE_ADMIN, ROLE_SELLER, ROLE_USER vs.)
    user_role: Optional[str]

    # Session bilgisi
    session_id: str

    # Intent classification sonucu
    intent: Optional[str]

    # Yönlendirilen agent adı
    selected_agent: Optional[str]

    # Frontend'e gönderilecek aksiyon tipi (PRODUCT_LIST, CART_UPDATED, vb.)
    action_type: Optional[str]

    # Frontend'e gönderilecek aksiyon verisi (ürün listesi, sepet bilgisi, vb.)
    action_data: Optional[Any]

    # Kullanıcıya gösterilecek nihai yanıt metni
    final_response: Optional[str]

    # Prompt injection tespit bayrağı — True ise güvenli yanıt verilir ve audit log düşülür
    injection_detected: bool

    # Hangi agent yanıt ürettiği (loglama ve frontend bilgisi için)
    agent_type: Optional[str]

    # İşlem sırasında oluşan hata (varsa)
    error: Optional[str]

    # Mevcut kullanıcı mesajı (raw — Supervisor'ın işlemek için kullandığı)
    current_message: str

    # ── Agentic UI Control — Onay ve İşlem Durumu ──

    # AI'ın kullanıcıdan onay isteyip istemediği
    requires_approval: bool

    # Onay bekleyen işlem planının JSON datasını tutar
    plan_data: Optional[str]

    # Backend'den alınan onay token'ı
    approval_token: Optional[str]

    # Onay durumu: None (henüz sorulmadı), PENDING, APPROVED, REJECTED, EXPIRED
    approval_status: Optional[str]

    # Çok adımlı işlem mi
    is_multi_step: bool

    # Çok adımlı işlem (AgentTransaction) ID'si
    transaction_id: Optional[int]

    # Mevcut adım numarası
    current_step: Optional[int]

    # Toplam adım sayısı
    total_steps: Optional[int]

    # Tamamlanan adımlar [{step, status, data}]
    completed_steps: list[dict]

    # Geri alma listesi
    rollback_actions: list[dict]

    # Ön doğrulama sonucu
    pre_validation_result: Optional[dict]
