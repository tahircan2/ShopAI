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
from agents.supervisor import supervisor_node, supervisor_respond, supervisor_profile_respond, route_to_agent
from agents.filter_agent import filter_agent_node
from agents.cart_agent import cart_agent_node
from agents.recommend_agent import recommend_agent_node
from agents.order_agent import order_agent_node
from agents.faq_agent import faq_agent_node
from agents.checkout_agent import checkout_agent_node
from agents.navigation_agent import navigation_agent_node
from agents.pre_validation_agent import pre_validation_agent
from agents.multi_step_executor import multi_step_executor_node
from agents.analytics_agent import analytics_agent_node
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
    graph.add_node("checkout_agent", checkout_agent_node)
    graph.add_node("navigation_agent", navigation_agent_node)
    graph.add_node("analytics_agent", analytics_agent_node)

    graph.add_node("multi_step_executor", multi_step_executor_node)
    graph.add_node("supervisor", supervisor_respond)  # GENERAL intent için
    graph.add_node("supervisor_profile", supervisor_profile_respond)  # USER_PROFILE intent için

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
            "checkout_agent": "checkout_agent",
            "navigation_agent": "navigation_agent",
            "multi_step_executor": "multi_step_executor",
            "supervisor_profile": "supervisor_profile",
            "supervisor": "supervisor",
            "analytics_agent": "analytics_agent",
        },
    )

    # Tüm agent node'ları END'e bağla
    graph.add_edge("filter_agent", END)
    graph.add_edge("cart_agent", END)
    graph.add_edge("recommend_agent", END)
    graph.add_edge("order_agent", END)
    graph.add_edge("faq_agent", END)
    graph.add_edge("checkout_agent", END)
    graph.add_edge("navigation_agent", END)
    graph.add_edge("analytics_agent", END)

    graph.add_edge("multi_step_executor", END)
    graph.add_edge("supervisor_profile", END)
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


async def stream_agent(
    message: str,
    session_id: str,
    user_id: str | None,
    user_role: str | None,
    conversation_history: list[dict] | None = None,
):
    """
    Ana agent çalıştırma fonksiyonu (Streaming).
    Önce LLM'den gelen tokenleri (chunk), ardından en son olarak güncellenmiş AgentState'i yield eder.

    Args:
        message: Kullanıcı mesajı (sanitize edilmiş, Spring Boot'tan gelmiş)
        session_id: Session UUID
        user_id: JWT'den extract edilen kullanıcı ID'si (None = anonim)
        user_role: JWT'den extract edilen kullanıcı rolü (None = anonim)
        conversation_history: Geçmiş mesajlar (DB'den — {role, content} listesi)

    Yields:
        {"type": "token", "content": "..."} veya {"type": "state", "state": AgentState}
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
        "user_role": user_role,      # JWT'den — kullanıcı girdisinden ASLA değil
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
        # Agentic UI Control fields
        "requires_approval": False,
        "plan_data": None,
        "approval_token": None,
        "approval_status": None,
        "is_multi_step": False,
        "transaction_id": None,
        "current_step": None,
        "total_steps": None,
        "completed_steps": [],
        "rollback_actions": [],
        "pre_validation_result": None,
        # Text2SQL Analytics fields
        "generated_sql": None,
        "sql_results": None,
        "analysis_text": None,
        "chart_config": None,
    }

    logger.info(
        "agent_run_start",
        session_id=session_id,
        user_id=user_id,
        message_preview=message[:50],
    )

    try:
        final_state = None
        
        async for mode, payload in graph.astream(
            initial_state,
            stream_mode=["messages", "values"],
            config={"recursion_limit": settings.langgraph_recursion_limit},
        ):
            if mode == "messages":
                chunk, metadata = payload
                # Yalnızca içeriği olan ve tool call içermeyen (yani kullanıcıya dönen) chunkları al
                if chunk.content and not getattr(chunk, "tool_calls", None) and getattr(chunk, "type", "") == "AIMessageChunk":
                    if "stream_to_user" in metadata.get("tags", []):
                        yield {"type": "token", "content": str(chunk.content)}
            elif mode == "values":
                final_state = payload

        if final_state:
            logger.info(
                "agent_run_complete",
                session_id=session_id,
                agent_type=final_state.get("agent_type"),
                intent=final_state.get("intent"),
                action_type=final_state.get("action_type"),
            )
            yield {"type": "state", "state": final_state}

    except Exception as e:
        logger.error("agent_run_error", session_id=session_id, error=str(e))
        # Hata state'i döndür
        error_state = {
            **initial_state,
            "final_response": "Bir hata oluştu. Lütfen tekrar deneyin.",
            "agent_type": "error",
            "error": str(e),
        }
        yield {"type": "state", "state": error_state}


async def run_agent(
    message: str,
    session_id: str,
    user_id: str | None,
    user_role: str | None,
    conversation_history: list[dict] | None = None,
) -> dict:
    """
    Non-streaming version of agent run. Returns the final state.
    """
    final_state = {}
    async for chunk in stream_agent(message, session_id, user_id, user_role, conversation_history):
        if chunk["type"] == "state":
            final_state = chunk["state"]
    return final_state
