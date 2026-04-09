"""
graph/agent_graph.py — LangGraph StateGraph tanımı ve bağlantıları.

Mimari:
  START → supervisor_node → [conditional routing] → {agent_node} → END

Conditional routing, supervisor'ın tespit ettiği intent'e göre yapılır.
recursion_limit ile sonsuz döngü önlenir.
"""

import structlog
from langgraph.graph import StateGraph, END, START
from langchain_core.messages import HumanMessage

from graph.state import AgentState
from agents.supervisor import supervisor_node, supervisor_respond, route_to_agent
from agents.filter_agent import filter_agent_node
from agents.cart_agent import cart_agent_node
from agents.recommend_agent import recommend_agent_node
from agents.order_agent import order_agent_node
from agents.faq_agent import faq_agent_node
from config import settings

logger = structlog.get_logger(__name__)


def build_graph() -> StateGraph:
    """
    LangGraph StateGraph'ı oluşturur ve döner.

    Akış:
      START
        ↓
      supervisor_node  ← intent classification + routing kararı
        ↓ (conditional edge: route_to_agent)
      ┌─────────────────────────────────────────┐
      │ filter_agent   │ cart_agent              │
      │ recommend_agent│ order_agent             │
      │ faq_agent      │ supervisor (GENERAL)    │
      └─────────────────────────────────────────┘
        ↓
      END
    """
    graph = StateGraph(AgentState)

    # Node'ları ekle
    graph.add_node("supervisor_node", supervisor_node)
    graph.add_node("filter_agent", filter_agent_node)
    graph.add_node("cart_agent", cart_agent_node)
    graph.add_node("recommend_agent", recommend_agent_node)
    graph.add_node("order_agent", order_agent_node)
    graph.add_node("faq_agent", faq_agent_node)
    graph.add_node("supervisor", supervisor_respond)  # GENERAL intent için

    # Başlangıç: supervisor'a git
    graph.add_edge(START, "supervisor_node")

    # Conditional routing: supervisor'ın kararına göre agent seç
    graph.add_conditional_edges(
        "supervisor_node",
        route_to_agent,
        {
            "filter_agent": "filter_agent",
            "cart_agent": "cart_agent",
            "recommend_agent": "recommend_agent",
            "order_agent": "order_agent",
            "faq_agent": "faq_agent",
            "supervisor": "supervisor",
        },
    )

    # Tüm agent node'ları END'e bağla
    graph.add_edge("filter_agent", END)
    graph.add_edge("cart_agent", END)
    graph.add_edge("recommend_agent", END)
    graph.add_edge("order_agent", END)
    graph.add_edge("faq_agent", END)
    graph.add_edge("supervisor", END)

    return graph.compile()


# Singleton graph instance
_compiled_graph = None


def get_graph():
    """
    Compiled graph singleton'ını döner.
    İlk çağrıda oluşturulur, sonrasında cache'den döner.
    """
    global _compiled_graph
    if _compiled_graph is None:
        logger.info("building_langgraph")
        _compiled_graph = build_graph()
        logger.info("langgraph_ready")
    return _compiled_graph


async def run_agent(
    message: str,
    session_id: str,
    user_id: str | None,
    conversation_history: list[dict] | None = None,
) -> AgentState:
    """
    Ana agent çalıştırma fonksiyonu.

    Args:
        message: Kullanıcı mesajı (sanitize edilmiş, Spring Boot'tan gelmiş)
        session_id: Session UUID
        user_id: JWT'den extract edilen kullanıcı ID'si (None = anonim)
        conversation_history: Geçmiş mesajlar (DB'den — {role, content} listesi)

    Returns:
        Tamamlanmış AgentState
    """
    graph = get_graph()

    # Konuşma geçmişini LangChain mesaj formatına dönüştür
    lc_history = []
    if conversation_history:
        from langchain_core.messages import AIMessage
        for msg in conversation_history[-settings.conversation_history_limit:]:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role == "user":
                lc_history.append(HumanMessage(content=content))
            elif role == "assistant":
                lc_history.append(AIMessage(content=content))

    # Mevcut kullanıcı mesajını geçmişe ekle
    lc_history.append(HumanMessage(content=message))

    # Başlangıç state'i oluştur
    initial_state: AgentState = {
        "messages": lc_history,
        "user_id": user_id,          # JWT'den — kullanıcı girdisinden ASLA değil
        "session_id": session_id,
        "current_message": message,
        "intent": None,
        "selected_agent": None,
        "action_type": None,
        "action_data": None,
        "final_response": None,
        "injection_detected": False,
        "agent_type": None,
        "error": None,
    }

    logger.info(
        "agent_run_start",
        session_id=session_id,
        user_id=user_id,
        message_preview=message[:50],
    )

    try:
        final_state = await graph.ainvoke(
            initial_state,
            config={"recursion_limit": settings.langgraph_recursion_limit},
        )

        logger.info(
            "agent_run_complete",
            session_id=session_id,
            agent_type=final_state.get("agent_type"),
            intent=final_state.get("intent"),
            action_type=final_state.get("action_type"),
        )

        return final_state

    except Exception as e:
        logger.error("agent_run_error", session_id=session_id, error=str(e))
        # Hata state'i döndür — uygulama çökmez
        return {
            **initial_state,
            "final_response": "Bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "error",
            "error": str(e),
        }
