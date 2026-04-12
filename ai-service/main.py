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
from fastapi.responses import JSONResponse

from config import settings
from models.schemas import ChatRequest, ChatResponse, HealthResponse
from security.prompt_guard import check_injection
from security.rate_limiter import rate_limiter
from graph.agent_graph import run_agent, get_graph

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


@app.post("/chat", response_model=ChatResponse, response_model_by_alias=True, tags=["Chat"])
async def chat(
    request: ChatRequest,
    x_authenticated_user_id: str | None = Header(
        default=None,
        alias="X-Authenticated-User-Id",
        description="Spring Boot'un JWT'den extract ettiği kullanıcı ID'si. "
                    "Anonim kullanıcılar için None.",
    ),
) -> ChatResponse:
    """
    Ana chat endpoint'i.

    Spring Boot bu endpoint'i proxy olarak çağırır.
    userId ASLA request body'den alınmaz — yalnızca X-Authenticated-User-Id header'ından.

    İşlem sırası:
    1. Rate limit kontrolü (session bazlı)
    2. Prompt injection kontrolü (3 katman — bu katman: kural + LLM)
    3. LangGraph agent pipeline
    4. Yanıtı döndür
    """
    session_id = request.session_id
    message = request.message

    # user_id: Spring Boot'un doğruladığı JWT'den — kullanıcı girdisinden ASLA değil
    user_id = x_authenticated_user_id

    logger.info(
        "chat_request_received",
        session_id=session_id,
        user_id=user_id,
        message_length=len(message),
    )

    # ---- 1. Rate Limit Kontrolü ----
    allowed, retry_after = await rate_limiter.is_allowed(session_id)
    if not allowed:
        logger.warning("rate_limit_hit", session_id=session_id, retry_after=retry_after)
        raise HTTPException(
            status_code=429,
            detail={
                "message": "Çok fazla mesaj gönderdiniz. Lütfen bir süre bekleyin.",
                "retry_after": retry_after,
            },
            headers={"Retry-After": str(retry_after)},
        )

    # ---- 2. Prompt Injection Kontrolü (Katman 3) ----
    injection_detected, safe_response = await check_injection(message)

    if injection_detected:
        logger.warning(
            "injection_blocked",
            session_id=session_id,
            user_id=user_id,
            message_preview=message[:100],
        )
        # Audit log için Spring Boot'a bildirim (fire-and-forget)
        # Not: Bu kısmı genişletmek için AuditNotifier servisi eklenebilir
        return ChatResponse(
            message=safe_response,
            agent_type="security",
            action_type=None,
            action_data=None,
            injection_detected=True,
            session_id=session_id,
            intent="BLOCKED",
        )

    # ---- 3. LangGraph Agent Pipeline ----
    final_state = await run_agent(
        message=message,
        session_id=session_id,
        user_id=user_id,
        conversation_history=None,  # Spring Boot geçmişi ayrıca geçirebilir
    )

    # ---- 4. Yanıtı Döndür ----
    response_message = final_state.get("final_response") or "Bir sorun oluştu, lütfen tekrar deneyin."

    return ChatResponse(
        message=response_message,
        agent_type=final_state.get("agent_type"),
        action_type=final_state.get("action_type"),
        action_data=final_state.get("action_data"),
        injection_detected=final_state.get("injection_detected", False),
        session_id=session_id,
        intent=final_state.get("intent"),
    )


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
