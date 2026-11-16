#!/usr/bin/env python3
"""Verify a standalone Valhalla graph tile archive before it is distributed."""
from __future__ import annotations
from pathlib import Path
import argparse,hashlib,json,tarfile,datetime,sys

def hash_file(p):
    h=hashlib.sha256()
    with p.open("rb") as s:
        for b in iter(lambda:s.read(4*1024*1024),b""): h.update(b)
    return h.hexdigest()

def inspect_tar(path):
    if path.stat().st_size<20*1024*1024:
        raise ValueError("Routing graph too small to represent Turkey")
    names=[]
    with tarfile.open(path,"r:") as t:
        for m in t:
            if m.isfile() and (m.name.endswith(".gph") or m.name.endswith(".gph.gz")):
                names.append(m.name)
                if len(names)>50:break
    if len(names)<5:raise ValueError("No Valhalla graph tiles found")
    return {"file_size":path.stat().st_size,"sample_tile_paths":names[:5]}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("graph",type=Path)
    p.add_argument("--source",type=Path,required=True)
    p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    result=inspect_tar(a.graph)
    result.update({"graph_sha256":hash_file(a.graph),
        "source_sha256":hash_file(a.source),
        "generator":"ghcr.io/valhalla/valhalla-scripted (un-pinned; pin after testing)",
        "scope":"Türkiye road routing (graph is required separately from Shortbread map)",
        "data_license":"© OpenStreetMap contributors · ODbL 1.0",
        "utc":datetime.datetime.now(datetime.timezone.utc).isoformat()})
    a.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=="__main__":
    try:main()
    except (ValueError,tarfile.TarError) as e: sys.exit("Invalid graph: "+str(e))
