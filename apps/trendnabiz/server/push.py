"""Optional self-hosted ntfy publisher (never exposes authorization secrets to APK)."""
from __future__ import annotations
import json
import os
import urllib.request
from urllib.parse import urlparse
from .store import Store

def publish(item: dict) -> bool:
    endpoint = os.getenv("TN_NTFY_BASE_URL", "").rstrip("/")
    token = os.getenv("TN_NTFY_TOKEN", "")
    if not endpoint or not token:
        return False
    u = urlparse(endpoint)
    if u.scheme != "https" or not u.hostname:
        raise ValueError("Publishing requires an HTTPS self-hosted ntfy URL")
    if not token.startswith("tk_"):
        raise ValueError("Expected ntfy bearer token")
    topic = os.getenv("TN_NTFY_TOPIC", "tn-public-events")
    if not topic.replace("-", "").replace("_","").isalnum():
        raise ValueError("Invalid ntfy topic")
    severity = item.get("severity", "normal")
    title = str(item.get("title", ""))[:200]
    url = str(item.get("url", ""))
    body = {
        "topic":topic,
        "title":title,
        "message":str(item.get("summary",""))[:500] + "\nSource: " + str(item.get("source",""))[:80],
        "priority": 5 if severity=="critical" else 4 if severity=="important" else 3,
        "click": url,
        "tags":["warning"] if severity=="critical" else ["newspaper"],
        "id":None
    }
    body.pop("id")
    request = urllib.request.Request(
        endpoint,
        data=json.dumps(body).encode("utf-8"),
        headers={"Authorization":"Bearer "+token,"Content-Type":"application/json","User-Agent":"TRENDNABIZ/0.2"},
        method="POST"
    )
    with urllib.request.urlopen(request,timeout=12) as response:
        return 200 <= response.status < 300

def flush(store: Store) -> dict:
    if not (os.getenv("TN_NTFY_BASE_URL") and os.getenv("TN_NTFY_TOKEN")):
        return {"sent":0,"failed":0,"disabled":True}
    sent=failed=0
    for record in store.pending():
        try:
            ok=publish(record["event"])
        except Exception:
            ok=False
        store.mark_delivery(record["id"],ok)
        sent+=int(ok)
        failed+=int(not ok)
    return {"sent":sent,"failed":failed,"disabled":False}
