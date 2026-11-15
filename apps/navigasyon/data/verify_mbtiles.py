#!/usr/bin/env python3
"""RaSaSeR Türkiye MBTiles yapısal kontrolü; yol/doğruluk testi DEĞİLDİR."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import gzip
import hashlib
import json
import os
from pathlib import Path
import sqlite3


class MapValidationError(ValueError):
    pass


def validate(path: Path) -> dict:
    path = path.resolve()
    if not path.is_file() or path.stat().st_size < 1024:
        raise MapValidationError("MBTiles yok veya geçerli boyutta değil")
    with path.open("rb") as f:
        if f.read(16) != b"SQLite format 3\x00":
            raise MapValidationError("SQLite dosyası değil")
    db = sqlite3.connect(f"file:{path.as_posix()}?mode=ro", uri=True)
    try:
        integrity = db.execute("PRAGMA quick_check").fetchone()
        if integrity is None or integrity[0] != "ok":
            raise MapValidationError(f"SQLite bozuk: {integrity}")
        objects = {
            row[0] for row in db.execute(
                "SELECT name FROM sqlite_master WHERE name IN ('metadata','tiles')"
            )
        }
        if not {"metadata", "tiles"} <= objects:
            raise MapValidationError("MBTiles metadata/tiles eksik")
        meta = dict(db.execute("SELECT name,value FROM metadata"))
        if meta.get("format") not in ("pbf", "mvt"):
            raise MapValidationError(f"Vektör karosu formatı bekleniyor: {meta.get('format')}")
        minzoom = int(meta.get("minzoom", "-1"))
        maxzoom = int(meta.get("maxzoom", "-1"))
        if minzoom < 0 or maxzoom < minzoom or maxzoom > 24:
            raise MapValidationError("Yakınlaştırma düzeyleri geçersiz")
        row = db.execute("SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles LIMIT 1").fetchone()
        if row is None or not row[3]:
            raise MapValidationError("Karo tablosu boş")
        z, x, y, data = row
        if z < 0 or x < 0 or y < 0 or x >= 2**z or y >= 2**z:
            raise MapValidationError("Karo koordinatları geçersiz")
        if data[:2] == b"\x1f\x8b":
            try:
                payload = gzip.decompress(data)
            except (OSError, EOFError) as exc:
                raise MapValidationError("Örnek gzip vektör karosu bozuk") from exc
        else:
            payload = data
        if len(payload) == 0:
            raise MapValidationError("Örnek vektör karosu boş")
        return {"format": meta["format"], "minzoom": minzoom, "maxzoom": maxzoom,
                "sample_zoom": z, "sample_size": len(payload),
                "name": meta.get("name", ""), "size_bytes": path.stat().st_size}
    except (sqlite3.DatabaseError, TypeError, ValueError) as exc:
        if isinstance(exc, MapValidationError):
            raise
        raise MapValidationError(str(exc)) from exc
    finally:
        db.close()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    p = argparse.ArgumentParser(description="MBTiles kalite ve kaynak bildirimi")
    p.add_argument("mbtiles", type=Path)
    p.add_argument("--source-sha256", required=True)
    p.add_argument("--manifest", required=True, type=Path)
    args = p.parse_args()
    if len(args.source_sha256) != 64 or any(c not in "0123456789abcdefABCDEF" for c in args.source_sha256):
        p.error("Geçerli SHA256 zorunlu")
    result = validate(args.mbtiles)
    manifest = {
        "project": "RaSaSeR Navigasyon Türkiye",
        "data_license": "ODbL 1.0 © OpenStreetMap contributors",
        "source_url": "https://download.geofabrik.de/europe/turkey-latest.osm.pbf",
        "source_sha256": args.source_sha256.lower(),
        "map_sha256": sha256_file(args.mbtiles),
        "verified_utc": datetime.now(timezone.utc).isoformat(),
        "checks": "structural_only_not_road_safety",
        "map": result,
    }
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    temp = args.manifest.with_name(args.manifest.name + ".tmp")
    temp.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temp, args.manifest)
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except MapValidationError as exc:
        raise SystemExit(f"Geçersiz MBTiles: {exc}") from exc
