"""
main.py — ShopAI AI Service FastAPI uygulaması.

Endpoint'ler:
  POST /chat  — Spring Boot proxy'sinden gelen chat isteklerini işler
  GET  /health — Servis sağlık kontrolü

KRİTİK GÜVENLİK KURALLARI:
1. userId ASLA request body'den alınmaz.
   Spring Boot'un doğruladığı JWT'den extract edilen değer
   X-Authenticated-User-Id header'ında gelir.
2. Kullanıcı girdisi sistem promptuna concat edilmez.
3. Her istek önce injection kontrolünden geçer.
4. Her session rate limit kontrolünden geçer.
"""

import structlog
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
import json

from config import settings
from models.schemas import ChatRequest, ChatResponse, HealthResponse
from security.prompt_guard import check_injection
from security.rate_limiter import rate_limiter
from graph.agent_graph import stream_agent, get_graph

# -------------------------------------------------------
# Logging yapılandırması
# -------------------------------------------------------
logging.basicConfig(level=getattr(logging, settings.log_level.upper(), logging.INFO))
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_log_level,
        structlog.stdlib.add_logger_name,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.dev.ConsoleRenderer(),
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
)

logger = structlog.get_logger(__name__)


# -------------------------------------------------------
# Uygulama yaşam döngüsü
# -------------------------------------------------------
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Uygulama başlangıcında graph'ı önceden derle."""
    logger.info("shopai_ai_service_starting", version="3.0.0")
    try:
        get_graph()  # Graph'ı önceden derle — ilk istek gecikmesini önle
        logger.info("langgraph_precompiled")
    except Exception as e:
        logger.error("graph_precompile_failed", error=str(e))
    yield
    logger.info("shopai_ai_service_shutdown")


# -------------------------------------------------------
# FastAPI uygulaması
# -------------------------------------------------------
app = FastAPI(
    title="ShopAI AI Service",
    description="LangGraph tabanlı e-ticaret AI agent servisi",
    version="3.0.0",
    docs_url="/docs",          # Production'da kapatılabilir
    redoc_url="/redoc",
    lifespan=lifespan,
)

# -------------------------------------------------------
# CORS — Yalnızca Spring Boot backend'den gelen isteklere izin ver
# -------------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.spring_boot_base_url.rstrip("/api").rstrip("/")],
    allow_credentials=True,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


# -------------------------------------------------------
# İç servis kimlik doğrulama middleware
# -------------------------------------------------------
@app.middleware("http")
async def verify_internal_key(request: Request, call_next):
    """
    Tüm isteklerde X-Internal-Key header'ını doğrular.
    Bu servis yalnızca Spring Boot'tan çağrılabilir.
    /health endpoint'i muaf tutulur.
    """
    if request.url.path == "/health":
        return await call_next(request)

    internal_key = request.headers.get("X-Internal-Key", "")
    if internal_key != settings.spring_boot_internal_key:
        logger.warning(
            "unauthorized_internal_access",
            path=request.url.path,
            client=request.client.host if request.client else "unknown",
        )
        return JSONResponse(
            status_code=403,
            content={"detail": "Bu endpoint yalnızca iç servisler tarafından erişilebilir."},
        )

    return await call_next(request)


# -------------------------------------------------------
# Endpoints
# -------------------------------------------------------

@app.get("/health", response_model=HealthResponse, tags=["System"])
async def health_check() -> HealthResponse:
    """Servis sağlık kontrolü — Spring Boot ve monitoring için."""
    return HealthResponse()


@app.post("/chat/stream", tags=["Chat"])
async def chat_stream(
    request: ChatRequest,
    x_authenticated_user_id: str | None = Header(
        default=None,
        alias="X-Authenticated-User-Id",
        description="Spring Boot'un JWT'den extract ettiği kullanıcı ID'si",
    ),
):
    session_id = request.session_id
    message = request.message
    user_id = x_authenticated_user_id

    logger.info("chat_stream_request", session_id=session_id)

    # 1. Rate Limit Kontrolü
    allowed, retry_after = await rate_limiter.is_allowed(session_id)
    if not allowed:
        raise HTTPException(
            status_code=429,
            detail={"message": "Çok fazla mesaj gönderdiniz", "retry_after": retry_after},
        )

    # 2. Prompt Injection Kontrolü
    injection_detected, safe_response = await check_injection(message)
    if injection_detected:
        async def mock_stream():
            yield f"data: {json.dumps({'type': 'token', 'content': safe_response})}\n\n"
            final_res = {
                "message": safe_response,
                "agent_type": "security",
                "action_type": None,
                "action_data": None,
                "injection_detected": True,
                "session_id": session_id,
                "intent": "BLOCKED",
            }
            yield f"data: {json.dumps({'type': 'state', 'state': final_res})}\n\n"
        return StreamingResponse(mock_stream(), media_type="text/event-stream")

    # 3. LangGraph Streaming Pipeline
    async def langgraph_stream():
        async for chunk in stream_agent(message, session_id, user_id):
            if chunk["type"] == "token":
                yield f"data: {json.dumps({'type': 'token', 'content': chunk['content']})}\n\n"
            elif chunk["type"] == "state":
                final_state = chunk["state"]
                response_message = final_state.get("final_response") or "Bir sorun oluştu."
                final_res = {
                    "message": response_message,
                    "agent_type": final_state.get("agent_type"),
                    "action_type": final_state.get("action_type"),
                    "action_data": final_state.get("action_data"),
                    "injection_detected": final_state.get("injection_detected", False),
                    "session_id": session_id,
                    "intent": final_state.get("intent"),
                }
                yield f"data: {json.dumps({'type': 'state', 'state': final_res})}\n\n"

    return StreamingResponse(langgraph_stream(), media_type="text/event-stream")


# -------------------------------------------------------
# Global hata handler
# -------------------------------------------------------
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """
    Yakalanmamış tüm hatalar için fallback handler.
    Stack trace veya iç mimari bilgisi ASLA response'a eklenmez.
    """
    logger.error(
        "unhandled_exception",
        path=request.url.path,
        method=request.method,
        error=str(exc),
        exc_info=True,
    )
    return JSONResponse(
        status_code=500,
        content={"detail": "Beklenmedik bir hata oluştu. Lütfen tekrar deneyin."},
    )


# -------------------------------------------------------
# Geliştirme sunucusu başlatma
# -------------------------------------------------------
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,          # Development — production'da False
        log_level=settings.log_level.lower(),
    )
