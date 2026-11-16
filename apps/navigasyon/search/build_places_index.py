#!/usr/bin/env python3
"""Build a real offline Turkey search index from the published Shortbread MBTiles.
© OpenStreetMap contributors, ODbL. Coordinates come ONLY from source tile geometry.
"""
import argparse,gzip,hashlib,json,math,re,sqlite3,unicodedata,zipfile
from pathlib import Path
import mapbox_vector_tile

def normalize(s):
    s=unicodedata.normalize("NFKD",s.lower().replace("ı","i"))
    return " ".join(re.findall(r"[^\W_]+","".join(ch for ch in s if not unicodedata.combining(ch))))
def digest(path):
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(4*1024*1024),b""):h.update(chunk)
    return h.hexdigest()
def pick(geo):
    t=geo.get("type");co=geo.get("coordinates")
    if not co:return None
    if t=="Point":return co
    if t in ("LineString","MultiPoint"):return co[len(co)//2]
    if t in ("Polygon","MultiLineString") and co[0]:return co[0][len(co[0])//2]
    return None
def gps(z,x,tms,xy,extent):
    if len(xy)!=2 or extent<=0:return None
    yy=(1<<z)-1-tms+float(xy[1])/extent
    lon=(x+float(xy[0])/extent)/(1<<z)*360-180
    lat=math.degrees(math.atan(math.sinh(math.pi*(1-2*yy/(1<<z)))))
    return (round(lat,6),round(lon,6)) if 35<=lat<=43.5 and 25<=lon<=46 else None
def build(src,out):
    if out.exists():out.unlink()
    db=sqlite3.connect(out)
    db.execute("PRAGMA journal_mode=OFF")
    db.execute("PRAGMA synchronous=OFF")
    db.execute("CREATE TABLE places(id INTEGER PRIMARY KEY,name TEXT NOT NULL,normalized TEXT NOT NULL,kind TEXT NOT NULL,lat REAL NOT NULL,lon REAL NOT NULL)")
    db.execute("CREATE UNIQUE INDEX places_unique ON places(normalized,kind,lat,lon)")
    db.execute("CREATE VIRTUAL TABLE places_fts USING fts4(normalized, content='places',tokenize=unicode61)")
    db.execute("CREATE TABLE info(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
    db.execute("INSERT INTO info VALUES('schema','1')")
    db.execute("INSERT INTO info VALUES('license','ODbL 1.0 © OpenStreetMap contributors')")
    source=sqlite3.connect(f"file:{src}?mode=ro",uri=True)
    count=0;seen=0;bad=0
    for z,x,y,data in source.execute("SELECT zoom_level,tile_column,tile_row,tile_data FROM tiles WHERE zoom_level IN (10,12,14) ORDER BY zoom_level DESC"):
        seen+=1
        if data[:2]==b"\x1f\x8b":data=gzip.decompress(data)
        try:layers=mapbox_vector_tile.decode(data,default_options={"y_coord_down":True})
        except Exception:
            bad+=1
            if bad>30:raise RuntimeError("Many invalid vector tiles")
            continue
        for name,layer in layers.items():
            if name not in ("pois","place_labels","street_labels","public_transport"):continue
            if name!="place_labels" and z!=14:continue
            extent=layer.get("extent",4096)
            for feat in layer.get("features",[]):
                properties=feat.get("properties") or {}
                title=properties.get("name") or properties.get("name_tr") or properties.get("name_en")
                if not isinstance(title,str) or not (2<=len(title)<=110):continue
                normalized=normalize(title)
                if len(normalized)<2:continue
                xy=pick(feat.get("geometry") or {})
                if xy is None:continue
                coords=gps(z,x,y,xy,extent)
                if coords is None:continue
                kind=str(properties.get("amenity") or properties.get("shop") or properties.get("kind") or name)[:60]
                cursor=db.execute("INSERT OR IGNORE INTO places(name,normalized,kind,lat,lon) VALUES(?,?,?,?,?)",(title,normalized,kind,*coords))
                if cursor.rowcount:
                    db.execute("INSERT INTO places_fts(docid,normalized) VALUES (?,?)",(cursor.lastrowid,normalized))
                    count+=1
        if seen%3000==0:
            db.commit()
            print(f"tiles={seen} records={count}",flush=True)
        if count>4_000_000:raise RuntimeError("Unexpected database size")
    db.commit()
    if count<1000:raise RuntimeError("Too few names: "+str(count))
    found=db.execute("SELECT count(*) FROM places_fts WHERE normalized MATCH 'ankara*'").fetchone()[0]
    if not found:raise RuntimeError("QA: Ankara not indexed")
    if db.execute("PRAGMA quick_check").fetchone()[0]!="ok":raise RuntimeError("SQLite corrupt")
    db.execute("INSERT INTO info VALUES('records',?)",(str(count),))
    db.commit();db.close();source.close()
    print(json.dumps(dict(tiles=seen,places=count,ankara=found,invalid=bad)),flush=True)

p=argparse.ArgumentParser()
for arg in ("mbtiles","output_db","zip","manifest"):p.add_argument(arg,type=Path)
a=p.parse_args()
build(a.mbtiles,a.output_db)
with zipfile.ZipFile(a.zip,"w",zipfile.ZIP_DEFLATED,compresslevel=6) as z:z.write(a.output_db,"turkey-places.sqlite")
result={"schema":1,"archive_sha256":digest(a.zip),"archive_bytes":a.zip.stat().st_size,
  "db_sha256":digest(a.output_db),"db_bytes":a.output_db.stat().st_size,
  "map_sha256":digest(a.mbtiles),"license":"ODbL 1.0 © OpenStreetMap contributors"}
a.manifest.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n")
print(json.dumps(result),flush=True)
