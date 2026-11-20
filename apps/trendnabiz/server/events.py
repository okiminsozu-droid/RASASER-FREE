"""Normalized event records and deduplication rules, independent of feed vendors."""
from __future__ import annotations
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
import hashlib
import re
from urllib.parse import urlparse

SEVERITIES = frozenset({"normal", "important", "critical"})

def timestamp(value: object, fallback: int = 0) -> int:
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        value = int(value)
        return value // 1000 if value > 10**11 else value
    if isinstance(value, str) and value:
        try:
            return int(datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp())
        except ValueError:
            return fallback
    return fallback

def short(text: object, limit: int) -> str:
    return re.sub(r"\s+", " ", str(text or "")).strip()[:limit]

@dataclass(frozen=True)
class Event:
    id: str
    title: str
    summary: str
    source: str
    url: str
    published_at: int
    category: str
    severity: str = "normal"
    region: str = ""
    status: str = "source_reported"

    def __post_init__(self) -> None:
        if not self.id or len(self.id) > 180:
            raise ValueError("Invalid event ID")
        if not self.title or len(self.title) > 280:
            raise ValueError("Invalid event title")
        if not self.source or len(self.source) > 90:
            raise ValueError("Invalid source")
        if not isinstance(self.published_at, int) or self.published_at <= 0:
            raise ValueError("Invalid event time")
        if self.severity not in SEVERITIES:
            raise ValueError("Invalid severity")
        if urlparse(self.url).scheme != "https":
            raise ValueError("Only HTTPS source URLs are allowed")
        if len(self.summary) > 750:
            raise ValueError("Summary is too long")

    def api(self) -> dict:
        return asdict(self)

def stable_id(namespace: str, external_id: str) -> str:
    """Constant-length ID; no secret tokens or feed-owned HTML retained."""
    digest = hashlib.sha256(external_id.encode("utf-8")).hexdigest()[:24]
    return f"{namespace}:{digest}"
