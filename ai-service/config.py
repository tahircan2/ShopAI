"""
config.py — Ortam değişkenleri ve uygulama ayarları.

Tüm hassas değerler .env dosyasından okunur.
Hiçbir secret değer bu dosyaya hardcode edilmez.
"""

from pydantic_settings import BaseSettings
from pydantic import Field
from functools import lru_cache


class Settings(BaseSettings):
    # OpenAI
    openai_api_key: str = Field(..., env="OPENAI_API_KEY")
    openai_model: str = Field(default="gpt-4o", env="OPENAI_MODEL")

    # Spring Boot backend URL (internal communication)
    spring_boot_base_url: str = Field(
        default="http://localhost:8080/api", env="SPRING_BOOT_BASE_URL"
    )
    spring_boot_internal_key: str = Field(..., env="SPRING_BOOT_INTERNAL_KEY")

    # Rate limiting
    max_messages_per_session_per_minute: int = Field(
        default=10, env="MAX_MESSAGES_PER_SESSION_PER_MINUTE"
    )
    conversation_history_limit: int = Field(
        default=10, env="CONVERSATION_HISTORY_LIMIT"
    )

    # Logging
    log_level: str = Field(default="INFO", env="LOG_LEVEL")

    # Redis (opsiyonel — rate limiter için)
    redis_url: str = Field(default="redis://localhost:6379", env="REDIS_URL")
    redis_enabled: bool = Field(default=False, env="REDIS_ENABLED")

    # Security
    max_message_length: int = Field(default=500, env="MAX_MESSAGE_LENGTH")

    # LangGraph
    langgraph_recursion_limit: int = Field(default=10, env="LANGGRAPH_RECURSION_LIMIT")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """Settings singleton — uygulama boyunca tek örnek döner."""
    return Settings()


# Global erişim kolaylığı
settings = get_settings()
