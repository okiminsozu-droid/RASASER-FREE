#!/usr/bin/env bash
# Türkiye ülke özütü -> offline vektör MBTiles üretim aracı.
# Bu komut büyük dosya indirir. Kullanıcı yerel makinede açıkça başlatmalıdır.
set -Eeuo pipefail
umask 022

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$HERE/work"
RAW="$WORK/raw"
ASSETS="$WORK/assets"
OUTPUT="$WORK/output"
ARCHIVE="$WORK/archive"
TEMP="$WORK/tmp"
mkdir -p "$RAW" "$ASSETS" "$OUTPUT" "$ARCHIVE" "$TEMP"

for cmd in curl unzip docker python3 md5sum sha256sum date; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "Gerekli komut eksik: $cmd" >&2; exit 2; }
done
docker info >/dev/null 2>&1 || { echo "Docker servisi çalışmıyor." >&2; exit 2; }

# Her zaman HTTPS; kaynak tek seferde indirilip checksum kontrolünden geçer.
download() {
  local url="$1" dest="$2"
  if [[ ! -s "$dest" ]]; then
    echo "İndiriliyor: $url"
    curl --proto '=https' --tlsv1.2 -fL --retry 4 --retry-delay 5 --connect-timeout 30 \
      "$url" -o "$dest.download"
    test -s "$dest.download"
    mv "$dest.download" "$dest"
  fi
}
BASE="https://download.geofabrik.de/europe"
PBF="$RAW/turkey-latest.osm.pbf"
download "$BASE/turkey-latest.osm.pbf" "$PBF"
# Aynı coğrafi anlık görüntünün MD5'i; değişen 'latest' için uyuşmazlıkta DUR.
curl --proto '=https' --tlsv1.2 -fL --retry 4 "$BASE/turkey-latest.osm.pbf.md5" -o "$RAW/turkey-latest.osm.pbf.md5.download"
mv "$RAW/turkey-latest.osm.pbf.md5.download" "$RAW/turkey-latest.osm.pbf.md5"
EXPECTED="$(awk 'NR==1 {print $1}' "$RAW/turkey-latest.osm.pbf.md5")"
ACTUAL="$(md5sum "$PBF" | awk '{print $1}')"
if [[ ! "$EXPECTED" =~ ^[0-9a-fA-F]{32}$ ]] || [[ "${EXPECTED,,}" != "$ACTUAL" ]]; then
  echo "OSM checksum uyuşmuyor: eski/güncellenen veri olabilir. Yanlış harita üretilmedi." >&2
  echo "Ham dosyayı doğruladıktan sonra tekrar indirin: $PBF" >&2
  exit 3
fi

COAST="$ASSETS/coastline"
LAND="$ASSETS/landcover"
mkdir -p "$COAST" "$LAND"
download "https://osmdata.openstreetmap.de/download/water-polygons-split-4326.zip" "$COAST/water-polygons-split-4326.zip"
unzip -oqj "$COAST/water-polygons-split-4326.zip" -d "$COAST"
test -s "$COAST/water_polygons.shp" || { echo "Kıyı .shp eksik" >&2; exit 4; }

for f in ne_10m_antarctic_ice_shelves_polys ne_10m_urban_areas ne_10m_glaciated_areas; do
  case "$f" in
    ne_10m_urban_areas) segment=cultural ;;
    *) segment=physical ;;
  esac
  download "https://naciscdn.org/naturalearth/10m/$segment/$f.zip" "$LAND/$f.zip"
  mkdir -p "$LAND/$f"
  unzip -oq "$LAND/$f.zip" -d "$LAND/$f"
  test -s "$LAND/$f/$f.shp" || { echo "Natural Earth .shp eksik: $f" >&2; exit 4; }
done

# Aynı dosyayı yerel depoda doğrudan uygulamaya değil, önce doğrulamaya sunuyoruz.
IMAGE="${TILEMAKER_IMAGE:-ghcr.io/systemed/tilemaker:master}"
BUILD_NAME="turkey.incomplete.mbtiles"
echo "Türkiye vektör harita üretiliyor. Docker resmi: $IMAGE"
docker run --rm \
  --mount "type=bind,src=$WORK,dst=/data" \
  --workdir /data \
  "$IMAGE" \
  /data/raw/turkey-latest.osm.pbf \
  --output "/data/output/$BUILD_NAME" \
  --store /data/tmp/tilemaker \
  --config /usr/src/app/config.json \
  --process /usr/src/app/process.lua

SOURCE_SHA="$(sha256sum "$PBF" | awk '{print $1}')"
python3 "$HERE/verify_mbtiles.py" "$OUTPUT/$BUILD_NAME" \
  --source-sha256 "$SOURCE_SHA" \
  --manifest "$OUTPUT/turkey.incomplete.manifest.json"

# Eski doğrulanmış çıktıya asla üzerine yazma / silme işlemi yapma.
if [[ -f "$OUTPUT/turkey.mbtiles" ]]; then
  STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
  mv "$OUTPUT/turkey.mbtiles" "$ARCHIVE/turkey-$STAMP.mbtiles"
  if [[ -f "$OUTPUT/turkey.manifest.json" ]]; then
    mv "$OUTPUT/turkey.manifest.json" "$ARCHIVE/turkey-$STAMP.manifest.json"
  fi
fi
mv "$OUTPUT/$BUILD_NAME" "$OUTPUT/turkey.mbtiles"
mv "$OUTPUT/turkey.incomplete.manifest.json" "$OUTPUT/turkey.manifest.json"
echo "Hazır: $OUTPUT/turkey.mbtiles"
echo "Kaynak/çıktı: $OUTPUT/turkey.manifest.json"
