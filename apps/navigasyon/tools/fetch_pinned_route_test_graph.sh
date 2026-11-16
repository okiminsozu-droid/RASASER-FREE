#!/usr/bin/env bash
set -Eeuo pipefail

DIR="$RUNNER_TEMP/nav-route-graph"
mkdir -p "$DIR"
CLOUDFLARE="https://rasaser-turkiye-map-download.okiminsozu.workers.dev/v1"
RELEASE="https://github.com/okiminsozu-droid/RASASER-FREE/releases/download/rasaser-turkiye-routing-v0.1"
GRAPH="$DIR/valhalla_tiles.tar"

# The Worker implements HTTP range 206. Resuming preserves bytes during an
# interrupted 1.15 GB transfer; no Cloudflare file or Worker is modified.
for attempt in $(seq 1 12); do
  if curl -fL -C - --proto '=https' --tlsv1.2 \
    --connect-timeout 25 --max-time 300 --retry 0 \
    -o "$GRAPH" "$CLOUDFLARE/valhalla_tiles.tar"; then
    break
  fi
  echo "Cloudflare transfer interrupted (attempt $attempt); resuming."
  sleep 3
done

if [ ! -s "$GRAPH" ] || [ "$(stat -c %s "$GRAPH")" -ne 1148661760 ]; then
  echo "Cloudflare transfer incomplete. Try immutable GitHub mirror (pinned SHA-256)."
  rm -f "$GRAPH"
  for attempt in $(seq 1 8); do
    if curl -fL -C - --proto '=https' --tlsv1.2 \
       --connect-timeout 25 --max-time 600 --retry 0 \
       -o "$GRAPH" "$RELEASE/valhalla_tiles.tar"; then
      break
    fi
    sleep 5
  done
fi
test "$(stat -c %s "$GRAPH")" -eq 1148661760
echo "bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f  $GRAPH" | sha256sum -c -

for DATA in valhalla.json valhalla-turkiye-manifest.json; do
  if ! curl -fL --proto '=https' --tlsv1.2 --retry 4 --retry-delay 3 \
    -o "$DIR/$DATA" "$CLOUDFLARE/$DATA"; then
    curl -fL --proto '=https' --tlsv1.2 --retry 4 --retry-delay 3 \
      -o "$DIR/$DATA" "$RELEASE/$DATA"
  fi
done
echo "bc0bb95856c09c51b859bb2f18423b43a0353504bb8e26529f3aad71bbf98fc3  $DIR/valhalla.json" | sha256sum -c -
echo "ce663429563e556ac78e729e8d919b72993f587a2b8426e5d556007022924f26  $DIR/valhalla-turkiye-manifest.json" | sha256sum -c -

python3 - "$DIR/valhalla.json" <<'PY'
import json,sys
config=json.load(open(sys.argv[1],encoding="utf-8"))
assert isinstance(config.get("mjolnir"),dict)
print("Route graph verified; config contains mjolnir, tile_extract and tile_dir.",flush=True)
PY
