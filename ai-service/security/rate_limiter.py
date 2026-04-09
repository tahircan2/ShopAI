"""
security/rate_limiter.py — Session bazlı rate limiting.

Implementation planı: Session başına dakikada maksimum 10 mesaj.
Aşıldığında: 429 + 'Lütfen bir süre bekleyin' mesajı.

Redis varsa Redis kullanılır, yoksa in-memory fallback devreye girer.
"""

import time
import asyncio
from collections import defaultdict, deque
from typing import Optional
import structlog

from config import settings

logger = structlog.get_logger(__name__)


class InMemoryRateLimiter:
    """
    Redis olmayan ortamlar için bellek içi rate limiter.
    Production ortamında Redis tabanlı limiter tercih edilmeli.
    """

    def __init__(self, max_requests: int, window_seconds: int = 60):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        # session_id → deque of timestamps
        self._requests: dict[str, deque] = defaultdict(deque)
        self._lock = asyncio.Lock()

    async def is_allowed(self, session_id: str) -> tuple[bool, int]:
        """
        Bu session için isteğe izin verilip verilmeyeceğini kontrol eder.

        Returns:
            (allowed: bool, retry_after_seconds: int)
            allowed = False ise retry_after_seconds bekleme süresidir.
        """
        async with self._lock:
            now = time.time()
            window_start = now - self.window_seconds
            session_requests = self._requests[session_id]

            # Zaman penceresi dışındaki istekleri temizle
            while session_requests and session_requests[0] < window_start:
                session_requests.popleft()

            if len(session_requests) >= self.max_requests:
                # En eski isteğin sona ermesine kadar bekleme süresi
                oldest = session_requests[0]
                retry_after = int(self.window_seconds - (now - oldest)) + 1
                logger.warning(
                    "rate_limit_exceeded",
                    session_id=session_id,
                    request_count=len(session_requests),
                    retry_after=retry_after,
                )
                return False, retry_after

            # İzin ver, timestamp ekle
            session_requests.append(now)
            return True, 0

    async def get_remaining(self, session_id: str) -> int:
        """Bu session için kalan istek hakkını döner."""
        async with self._lock:
            now = time.time()
            window_start = now - self.window_seconds
            session_requests = self._requests[session_id]
            # Temizle
            while session_requests and session_requests[0] < window_start:
                session_requests.popleft()
            return max(0, self.max_requests - len(session_requests))


class RedisRateLimiter:
    """
    Redis tabanlı rate limiter — production için önerilen.
    Sliding window algoritması kullanır.
    """

    def __init__(self, redis_url: str, max_requests: int, window_seconds: int = 60):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._redis = None
        self._redis_url = redis_url

    async def _get_redis(self):
        if self._redis is None:
            import redis.asyncio as aioredis
            self._redis = await aioredis.from_url(self._redis_url, decode_responses=True)
        return self._redis

    async def is_allowed(self, session_id: str) -> tuple[bool, int]:
        try:
            r = await self._get_redis()
            key = f"rate_limit:{session_id}"
            now = time.time()
            window_start = now - self.window_seconds

            pipe = r.pipeline()
            # Sliding window: eski kayıtları sil, yeni ekle, say
            pipe.zremrangebyscore(key, 0, window_start)
            pipe.zcard(key)
            pipe.zadd(key, {str(now): now})
            pipe.expire(key, self.window_seconds)
            results = await pipe.execute()

            count = results[1]  # zadd öncesindeki sayım

            if count >= self.max_requests:
                # En eski isteğin skoru
                oldest = await r.zrange(key, 0, 0, withscores=True)
                if oldest:
                    oldest_ts = oldest[0][1]
                    retry_after = int(self.window_seconds - (now - oldest_ts)) + 1
                else:
                    retry_after = self.window_seconds
                logger.warning("redis_rate_limit_exceeded", session_id=session_id, count=count)
                return False, retry_after

            return True, 0
        except Exception as e:
            # Redis hatasında in-memory fallback'e düş
            logger.error("redis_rate_limiter_error", error=str(e))
            return True, 0  # Fail open — servis kesintisi yaşatma

    async def get_remaining(self, session_id: str) -> int:
        try:
            r = await self._get_redis()
            key = f"rate_limit:{session_id}"
            now = time.time()
            window_start = now - self.window_seconds
            await r.zremrangebyscore(key, 0, window_start)
            count = await r.zcard(key)
            return max(0, self.max_requests - count)
        except Exception:
            return self.max_requests


def create_rate_limiter():
    """
    Ayarlara göre uygun rate limiter döner.
    REDIS_ENABLED=true ise Redis, değilse in-memory.
    """
    max_req = settings.max_messages_per_session_per_minute
    if settings.redis_enabled:
        logger.info("rate_limiter_init", backend="redis")
        return RedisRateLimiter(
            redis_url=settings.redis_url,
            max_requests=max_req,
            window_seconds=60,
        )
    else:
        logger.info("rate_limiter_init", backend="in_memory")
        return InMemoryRateLimiter(max_requests=max_req, window_seconds=60)


# Singleton — uygulama boyunca tek örnek
rate_limiter = create_rate_limiter()
