"""Read-only HTTP/JSON and optional foreground SSE API; reverse-proxy HTTPS in production."""
from __future__ import annotations
import json
import os
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs
from .store import Store

class NewsHandler(BaseHTTPRequestHandler):
    store: Store

    def log_message(self, fmt: str, *args) -> None:
        # Do not write query strings or device identifiers to application logs.
        print("HTTP", self.command, urlsplit(self.path).path, *args[:1], flush=True)

    def json_response(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        parsed = urlsplit(self.path)
        args = parse_qs(parsed.query, keep_blank_values=False)
        if parsed.path == "/healthz":
            self.json_response(200, {"service":"TRENDNABIZ", "status":"running", "sources":self.store.source_status()})
            return
        if parsed.path == "/v1/events":
            try:
                limit = max(1,min(int(args.get("limit",["50"])[0]),100))
                after = max(0, int(args.get("after",["0"])[0]))
            except ValueError:
                self.json_response(400,{"error":"invalid_query"})
                return
            stories = self.store.list_events(limit=limit, after=after)
            self.json_response(200, {"version":1,"events":stories, "count":len(stories)})
            return
        if parsed.path == "/v1/stream":
            self.stream_events()
            return
        self.json_response(404,{"error":"not_found"})

    def stream_events(self) -> None:
        # For an actively OPEN app only. It does not keep Android alive when backgrounded.
        self.send_response(200)
        self.send_header("Content-Type","text/event-stream; charset=utf-8")
        self.send_header("Cache-Control","no-cache")
        self.send_header("X-Accel-Buffering","no")
        self.end_headers()
        try:
            last = int(time.time())
            delivered = set()
            for _ in range(24):  # max 2-minute connection; client reconnects
                news = self.store.created_since(last)
                for event in news:
                    if event["id"] in delivered: continue
                    delivered.add(event["id"])
                    body = json.dumps(event,ensure_ascii=False,separators=(",",":"))
                    self.wfile.write(("event: story\ndata: "+body+"\n\n").encode("utf-8"))
                last = max(last, int(time.time())-1)
                self.wfile.write(b": ping\n\n")
                self.wfile.flush()
                time.sleep(5)
        except (BrokenPipeError, ConnectionResetError, OSError):
            pass

def serve(store: Store) -> None:
    # Bind loopback by default: expose only through an HTTPS reverse proxy.
    addr=os.getenv("TN_BIND","127.0.0.1")
    port=int(os.getenv("TN_PORT","8808"))
    subclass=type("TrendNabizHandler",(NewsHandler,),{"store":store})
    server=ThreadingHTTPServer((addr,port),subclass)
    print(f"TRENDNABIZ listening on {addr}:{port} (terminate with Ctrl-C)",flush=True)
    try: server.serve_forever(poll_interval=.5)
    except KeyboardInterrupt: pass
    finally: server.server_close()
