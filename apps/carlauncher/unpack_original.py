#!/usr/bin/env python3
"""Recreate the user's original ZIP sources byte-for-byte, without changing Kotlin code."""
from pathlib import Path
from hashlib import sha256
import base64
import brotli
import sys

ROOT = Path(__file__).resolve().parent
PARTS = ROOT / "source-packed"
OUTPUT = ROOT / "original"
EXPECTED_SHA256 = "52f901cdcf37427f6b6e4bcdf7b4a1509ae9cedd7d8e97badebf7ca325374b4d"
EXPECTED_FILES = 10
files = sorted(PARTS.glob("part*.txt"))
if len(files) != 6:
    raise SystemExit(f"Expected 6 source parts; got {len(files)}")
encoded = "".join(f.read_text(encoding="ascii").strip() for f in files)
source = brotli.decompress(base64.b64decode(encoded, validate=True))
digest = sha256(source).hexdigest()
if digest != EXPECTED_SHA256:
    raise SystemExit("Original ZIP source integrity hash mismatch: " + digest)
position, count = 0, 0
while position < len(source):
    end = source.find(b"\x00", position)
    if end < 0 or end + 5 > len(source):
        raise SystemExit("Corrupt source record")
    path = source[position:end].decode("utf-8")
    length = int.from_bytes(source[end + 1 : end + 5], "big")
    start = end + 5
    data = source[start : start + length]
    if len(data) != length:
        raise SystemExit("Corrupt source length")
    target = OUTPUT / path
    if not path or path.startswith("/") or ".." in Path(path).parts or target.resolve().is_relative_to(OUTPUT.resolve()) is False:
        raise SystemExit("Unsafe source path")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    position = start + length
    count += 1
if count != EXPECTED_FILES:
    raise SystemExit(f"Expected {EXPECTED_FILES} files; got {count}")
print(f"Restored {count} unchanged Android Studio source files; SHA256={digest}")
