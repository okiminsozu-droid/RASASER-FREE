"""Ingestion loop, source-level failure isolation and durable push outbox."""
from __future__ import annotations
import json
import os
import time
import threading
from pathlib import Path
from .feeds import approved_feed_events,fetch_bytes,fetch_json,provider_specs
from .store import Store
from .push import flush
from .api import serve

POLL = {"usgs":60, "noaa":180, "nasa":300, "approved_feeds":300}

def process_sources(store: Store, now: int | None = None, which: set[str] | None = None) -> dict:
    now = now or int(time.time())
    report = {}
    for name,(url,adapter) in provider_specs().items():
        if which is not None and name not in which:
            continue
        if name=="noaa" and os.getenv("TN_ENABLE_NOAA","1") != "1":
            continue
        try:
            events=adapter(fetch_json(url),now)
            report[name]=store.ingest(name,events,now)
        except Exception as exc:
            store.source_error(name, f"{type(exc).__name__}: {exc}")
            report[name]={"error":type(exc).__name__}
    if which is None or "approved_feeds" in which:
        config_path=os.getenv("TN_FEEDS_CONFIG","")
        if config_path:
            try:
                entries=json.loads(Path(config_path).read_text(encoding="utf-8"))
                if not isinstance(entries,list):
                    raise ValueError("feed config must be a JSON list")
                for feed in entries[:35]:
                    if not (feed.get("enabled") is True and feed.get("permission_recorded") is True
                            and feed.get("title_and_summary_reuse_allowed") is True):
                        continue
                    name="licensed:"+str(feed["name"])[:80]
                    try:
                        events=approved_feed_events(fetch_bytes(feed["url"]),feed,now)
                        report[name]=store.ingest(name,events,now)
                    except Exception as exc:
                        store.source_error(name,f"{type(exc).__name__}: {exc}")
                        report[name]={"error":type(exc).__name__}
            except Exception as exc:
                report["approved_feeds"]={"error":type(exc).__name__}
    report["ntfy"]=flush(store)
    store.prune()
    return report

def loop(store: Store) -> None:
    last={}
    while True:
        now=int(time.time())
        due={name for name,sec in POLL.items() if now-last.get(name,0)>=sec}
        if due:
            process_sources(store,now,due)
            for name in due:
                last[name]=now
        else:
            flush(store)
        time.sleep(5)

def main() -> None:
    store=Store(os.getenv("TN_DATABASE","./runtime/trendnabiz.db"))
    if "--once" in __import__("sys").argv:
        print(json.dumps(process_sources(store),ensure_ascii=False,indent=2))
        return
    if "--api-only" not in __import__("sys").argv:
        worker=threading.Thread(target=loop,args=(store,),name="trendnabiz-ingestion",daemon=True)
        worker.start()
    serve(store)

if __name__=="__main__":
    main()
