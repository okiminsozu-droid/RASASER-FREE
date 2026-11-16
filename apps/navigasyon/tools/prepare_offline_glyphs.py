#!/usr/bin/env python3
"""Bundle OFL-licensed Noto Sans vector-glyph PBFs for 100% offline map labels."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import argparse, hashlib, urllib.request, time, os

URL="https://raw.githubusercontent.com/UNDP-Data/fonts/main/docs/fonts/Noto%20Sans%20Regular"
LICENSE_URL=URL+"/LICENSE"
def get(url):
    for attempt in range(4):
        try:
            req=urllib.request.Request(url,headers={"User-Agent":"RaSaSeR-Navigasyon/offline-glyph-builder"})
            with urllib.request.urlopen(req,timeout=40) as response:
                data=response.read(1024*1024+1)
                if not (20 <= len(data) < 1024*1024):
                    raise ValueError("Bad glyph file size")
                return data
        except Exception:
            if attempt==3: raise
            time.sleep(attempt+1)
def download_one(idx,directory):
    start=idx*256
    name=f"{start}-{start+255}.pbf"
    dest=directory/name
    if dest.is_file() and dest.stat().st_size>=20:
        return dest.stat().st_size
    content=get(URL+"/"+name)
    temp=dest.with_suffix(".download")
    temp.write_bytes(content)
    os.replace(temp,dest)
    return len(content)
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--target",type=Path,default=Path("app/src/main/assets/fonts/Noto Sans Regular"))
    a=p.parse_args(); a.target.mkdir(parents=True,exist_ok=True)
    # Exactly 256 Unicode 256-codepoint blocks: offline, no font CDN at runtime.
    total=0
    with ThreadPoolExecutor(max_workers=12) as executor:
        futures=[executor.submit(download_one,i,a.target) for i in range(256)]
        for result in as_completed(futures):
            total+=result.result()
    license_file=a.target/"LICENSE"
    license_file.write_bytes(get(LICENSE_URL))
    if total > 25*1024*1024: raise ValueError("Unexpected glyph size: stop build")
    if len(list(a.target.glob("*.pbf")))!=256: raise ValueError("Incomplete glyph ranges")
    print(f"Offline Noto Sans glyphs ready: 256 blocks, {total} bytes")
if __name__=="__main__":
    main()
