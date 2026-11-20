"""SQLite-backed immutable event log and idempotent notification outbox."""
from __future__ import annotations
import json
import sqlite3
import threading
import time
from pathlib import Path
from .events import Event

SCHEMA = """
CREATE TABLE IF NOT EXISTS events (
 id TEXT PRIMARY KEY, published_at INTEGER NOT NULL, created_at INTEGER NOT NULL,
 category TEXT NOT NULL, severity TEXT NOT NULL, payload TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_events_time ON events(published_at DESC);
CREATE TABLE IF NOT EXISTS source_states (
 name TEXT PRIMARY KEY, initialized INTEGER NOT NULL DEFAULT 0,
 last_success INTEGER, last_error TEXT
);
CREATE TABLE IF NOT EXISTS outbox (
 id TEXT PRIMARY KEY, created_at INTEGER NOT NULL, delivered_at INTEGER,
 attempts INTEGER NOT NULL DEFAULT 0, next_attempt INTEGER NOT NULL,
 FOREIGN KEY(id) REFERENCES events(id)
);
"""

class Store:
    def __init__(self, path: str) -> None:
        self.path = path
        self.lock = threading.RLock()
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as db:
            db.executescript(SCHEMA)

    def connect(self):
        db = sqlite3.connect(self.path, timeout=10)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA busy_timeout=10000")
        return db

    def ingest(self, source_name: str, items: list[Event], now: int | None = None) -> dict:
        now = now or int(time.time())
        with self.lock, self.connect() as db:
            db.execute("BEGIN IMMEDIATE")
            state = db.execute("SELECT initialized FROM source_states WHERE name=?", (source_name,)).fetchone()
            initialized = bool(state and state["initialized"])
            new_count = 0
            queued = 0
            for event in items:
                cursor = db.execute(
                    "INSERT OR IGNORE INTO events(id,published_at,created_at,category,severity,payload) VALUES(?,?,?,?,?,?)",
                    (event.id,event.published_at,now,event.category,event.severity,json.dumps(event.api(),ensure_ascii=False))
                )
                if cursor.rowcount:
                    new_count += 1
                    if initialized and now - event.published_at <= 40*60:
                        # Initial synchronization intentionally never sends a backlog to subscribers.
                        db.execute("INSERT OR IGNORE INTO outbox(id,created_at,next_attempt) VALUES(?,?,?)",
                                   (event.id, now, now))
                        queued += 1
            db.execute("""INSERT INTO source_states(name,initialized,last_success,last_error)
                          VALUES(?,1,?,NULL)
                          ON CONFLICT(name) DO UPDATE SET initialized=1,last_success=excluded.last_success,last_error=NULL""",
                       (source_name, now))
            db.commit()
            return {"new":new_count,"queued":queued,"baseline":not initialized}

    def source_error(self, name: str, detail: str) -> None:
        with self.lock, self.connect() as db:
            db.execute("""INSERT INTO source_states(name,initialized,last_error) VALUES(?,0,?)
                          ON CONFLICT(name) DO UPDATE SET last_error=excluded.last_error""",
                       (name,detail[:200]))
            db.commit()

    def list_events(self, limit: int = 50, after: int = 0) -> list[dict]:
        with self.lock, self.connect() as db:
            rows = db.execute(
                "SELECT payload FROM events WHERE published_at>? ORDER BY published_at DESC LIMIT ?",
                (after,max(1,min(limit,100)))
            ).fetchall()
            return [json.loads(r["payload"]) for r in rows]

    def created_since(self, after: int, limit: int = 50) -> list[dict]:
        """For SSE clients: server-inserted events, not provider publication clock."""
        with self.lock, self.connect() as db:
            rows = db.execute(
                "SELECT payload FROM events WHERE created_at>=? ORDER BY created_at,id LIMIT ?",
                (after,max(1,min(limit,100)))
            ).fetchall()
            return [json.loads(r["payload"]) for r in rows]

    def source_status(self) -> list[dict]:
        with self.lock, self.connect() as db:
            return [dict(r) for r in db.execute("SELECT name,initialized,last_success,last_error FROM source_states ORDER BY name")]

    def pending(self, now: int | None = None, limit: int = 30) -> list[dict]:
        now = now or int(time.time())
        with self.lock, self.connect() as db:
            rows = db.execute("""
              SELECT o.id,e.payload,o.attempts FROM outbox o JOIN events e USING(id)
              WHERE o.delivered_at IS NULL AND o.next_attempt<=?
              ORDER BY o.created_at LIMIT ?
            """,(now,limit)).fetchall()
            return [{"id":r["id"],"event":json.loads(r["payload"]),"attempts":r["attempts"]} for r in rows]

    def mark_delivery(self, event_id: str, success: bool, now: int | None = None) -> None:
        now = now or int(time.time())
        with self.lock, self.connect() as db:
            if success:
                db.execute("UPDATE outbox SET delivered_at=?, attempts=attempts+1 WHERE id=?", (now,event_id))
            else:
                row = db.execute("SELECT attempts FROM outbox WHERE id=?", (event_id,)).fetchone()
                attempts = (row["attempts"] if row else 0)+1
                wait = min(3600,15 * 2**min(attempts,7))
                db.execute("UPDATE outbox SET attempts=?,next_attempt=? WHERE id=?",(attempts,now+wait,event_id))
            db.commit()

    def prune(self, max_age_days: int = 14) -> None:
        cutoff = int(time.time())-max_age_days*86400
        with self.lock,self.connect() as db:
            db.execute("DELETE FROM outbox WHERE id IN (SELECT id FROM events WHERE published_at<?)",(cutoff,))
            db.execute("DELETE FROM events WHERE published_at<?",(cutoff,))
            db.commit()
