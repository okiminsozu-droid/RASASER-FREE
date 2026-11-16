#!/usr/bin/env python3
"""Download and verify countrywide Shortbread MBTiles. NOT a routing graph."""
from __future__ import annotations
import argparse, datetime, hashlib, json, math, shutil, sqlite3, subprocess
from pathlib import Path
from zipfile import ZipFile

SOURCE="https://data.bbbike.org/osm/mbtiles/region/europe/turkey/turkey.osm.mbtiles-shortbread.zip"
CITIES={"Istanbul":(28.9784,41.0082),"Ankara":(32.8597,39.9334),
        "Izmir":(27.1428,38.4237),"Usak":(29.4058,38.6823),
        "Van":(43.3730,38.4942),"Edirne":(26.5557,41.6772),
        "Hatay":(36.1619,36.2021),"Kars":(43.0975,40.6167),
        "Mardin":(40.7351,37.3129),"Antalya":(30.7133,36.8969)}
def sha256(path):
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(4*1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()
def coordinate(lon,lat,z):
    n=1<<z
    x=int((lon+180)/360*n)
    y=int((1-math.asinh(math.tan(math.radians(lat)))/math.pi)*n/2)
    return x,n-y-1  # MBTiles TMS row
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--output",type=Path,default=Path("work/turkey-full"))
    p.add_argument("--source",default=SOURCE)
    a=p.parse_args(); output=a.output.resolve();output.mkdir(parents=True,exist_ok=True)
    archive=output/"rasaser-turkiye-shortbread.zip"
    mbtiles=output/"turkey.mbtiles"
    manifest=output/"turkey.manifest.json"
    subprocess.run(["curl","--fail","--location","--retry","4","--retry-delay","8",
        "--connect-timeout","30","--speed-time","180","--speed-limit","4096",
        "--continue-at","-","--show-error","--output",str(archive),a.source],check=True)
    if archive.stat().st_size < 100*1024*1024:
        raise ValueError("Invalid small Türkiye archive")
    with ZipFile(archive) as z:
        files=[m for m in z.infolist() if m.filename.lower().endswith(".mbtiles") and not m.is_dir()]
        if len(files)!=1 or files[0].file_size < 100*1024*1024:
            raise ValueError("ZIP must contain one full-sized MBTiles file")
        # Streaming extraction also validates CRC; never extract paths from archive.
        with z.open(files[0]) as src, mbtiles.open("wb") as dst:
            shutil.copyfileobj(src,dst,4*1024*1024)
    con=sqlite3.connect("file:"+mbtiles.as_posix()+"?mode=ro",uri=True)
    try:
        if con.execute("PRAGMA quick_check").fetchone()[0]!="ok": raise ValueError("SQLite corrupt")
        objects={row[0] for row in con.execute("SELECT name FROM sqlite_master")}
        if not {"tiles","metadata"}.issubset(objects): raise ValueError("Invalid MBTiles objects")
        meta=dict(con.execute("SELECT name,value FROM metadata"))
        if meta.get("format") not in ("pbf","mvt"): raise ValueError("Not vector tiles")
        maxzoom=int(meta.get("maxzoom","14")); minzoom=int(meta.get("minzoom","0"))
        count=con.execute("SELECT count(*) FROM tiles").fetchone()[0]
        if count<1000 or maxzoom<9: raise ValueError("Insufficient tiles / zoom")
        sample={}
        for city,(lon,lat) in CITIES.items():
            x,y=coordinate(lon,lat,min(10,maxzoom))
            sample[city]=bool(con.execute(
              "SELECT 1 FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
              (min(10,maxzoom),x,y)).fetchone())
        if not all(sample.values()):
            raise ValueError("Nationwide coverage check failed: "+str(sample))
        details={"size_bytes":mbtiles.stat().st_size,"format":meta["format"],
                 "minzoom":minzoom,"maxzoom":maxzoom,"tile_count":count,
                 "bounds":meta.get("bounds"),"sample_city_zoom":min(10,maxzoom),
                 "sample_cities":sample,"schema":"Shortbread","scheme":"TMS"}
    finally:
        con.close()
    record={"project":"RaSaSeR Navigasyon Türkiye","scope":"countrywide vector basemap; NOT a routing graph",
      "source_provider":"BBBike.org","source_url":a.source,
      "license":"© OpenStreetMap contributors, ODbL 1.0",
      "source_archive_sha256":sha256(archive),"map_sha256":sha256(mbtiles),
      "map":details,"verified_utc":datetime.datetime.now(datetime.timezone.utc).isoformat()}
    manifest.write_text(json.dumps(record,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps(record,indent=2,ensure_ascii=False))
if __name__=="__main__": main()
