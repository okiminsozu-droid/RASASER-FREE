#!/usr/bin/env python3
"""Independent offline check for privately repackaged Quran APKs.

Run after APK signing: python3 apps/kurankelime/tools/check_apk_install_alignment.py <file.apk>
For targetSdk >=30, resources.arsc must be uncompressed and 4-byte aligned.
Uncompressed native .so libraries should be 16KB aligned for Android 15 devices.
Does not inspect user content or private signing secrets.
"""
import struct
import sys
from pathlib import Path
from zipfile import ZipFile, ZIP_STORED

def validate(path):
    with ZipFile(path) as apk, open(path, "rb") as fh:
        names=apk.namelist()
        assert len(names)==len(set(names)), "Duplicate archive entries"
        assert apk.testzip() is None, "Corrupt ZIP entry"
        assert "resources.arsc" in names, "Missing Android resource table"
        checked=0
        for entry in apk.infolist():
            if entry.compress_type!=ZIP_STORED:
                if entry.filename=="resources.arsc":
                    raise ValueError("resources.arsc is compressed; Android 11+ rejects it")
                continue
            fh.seek(entry.header_offset+26)
            name_len, extra_len=struct.unpack("<HH",fh.read(4))
            offset=entry.header_offset+30+name_len+extra_len
            boundary=16384 if entry.filename.startswith("lib/") and entry.filename.endswith(".so") else 4
            if offset%boundary:
                raise ValueError(
                    f"{entry.filename} offset {offset} not aligned to {boundary} bytes")
            checked+=1
        print(f"Aligned: {checked} STORED entries; resources.arsc 4-byte; native libs 16KB")
if __name__=="__main__":
    if len(sys.argv)!=2:
        print("Usage: check_apk_install_alignment.py signed.apk",file=sys.stderr)
        sys.exit(2)
    try:
        validate(Path(sys.argv[1]))
    except Exception as exc:
        print(f"INSTALLATION RISK: {exc}",file=sys.stderr)
        sys.exit(1)
