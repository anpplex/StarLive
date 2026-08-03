#!/usr/bin/env bash
# Build a StarLive theme pack zip: catalog.json + image(s).
#
# Usage:
#   ./scripts/make-theme-pack.sh --id pack_x --title "标题" --image ./a.jpg --out ./pack_x.zip
#   ./scripts/make-theme-pack.sh --id pack_x --title "标题" \
#     --image ./dark.jpg --label "深" --image2 ./light.jpg --label2 "浅" --out ./pack_x.zip
set -euo pipefail

ID=""
TITLE=""
IMAGE=""
LABEL=""
IMAGE2=""
LABEL2=""
OUT=""
VERSION=1

usage() {
  sed -n '2,8p' "$0"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ID="$2"; shift 2 ;;
    --title) TITLE="$2"; shift 2 ;;
    --image) IMAGE="$2"; shift 2 ;;
    --label) LABEL="$2"; shift 2 ;;
    --image2) IMAGE2="$2"; shift 2 ;;
    --label2) LABEL2="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --version) VERSION="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$ID" || -z "$IMAGE" || -z "$OUT" ]]; then
  echo "Required: --id --image --out" >&2
  usage
  exit 1
fi
if [[ ! -f "$IMAGE" ]]; then
  echo "Image not found: $IMAGE" >&2
  exit 1
fi
if [[ -n "$IMAGE2" && ! -f "$IMAGE2" ]]; then
  echo "Image2 not found: $IMAGE2" >&2
  exit 1
fi

TITLE="${TITLE:-$ID}"
LABEL="${LABEL:-$TITLE}"
LABEL2="${LABEL2:-${TITLE}-2}"

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

python3 - "$ID" "$TITLE" "$VERSION" "$IMAGE" "$LABEL" "$IMAGE2" "$LABEL2" "$WORKDIR" <<'PY'
import json, os, shutil, sys
from pathlib import Path

pack_id, title, version, image, label, image2, label2, work = sys.argv[1:9]
work = Path(work)
version = int(version)

def ext_of(p: str) -> str:
    e = Path(p).suffix.lower().lstrip(".")
    if e == "jpeg":
        e = "jpg"
    if e not in ("jpg", "png", "webp"):
        raise SystemExit(f"Unsupported image type: {e or '(none)'}")
    return e

items = []
e1 = ext_of(image)
f1 = f"wallpaper.{e1}"
shutil.copy2(image, work / f1)
items.append({"id": "main", "file": f1, "label": label})

if image2:
    e2 = ext_of(image2)
    f2 = f"wallpaper_2.{e2}"
    shutil.copy2(image2, work / f2)
    items.append({"id": "alt", "file": f2, "label": label2})

catalog = {
    "pack_id": pack_id,
    "title": title,
    "version": version,
    "wallpapers": items,
}
(work / "catalog.json").write_text(
    json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print("catalog:", json.dumps(catalog, ensure_ascii=False))
PY

mkdir -p "$(dirname "$OUT")"
# Resolve absolute out path before cd
case "$OUT" in
  /*) OUT_ABS="$OUT" ;;
  *) OUT_ABS="$(pwd)/$OUT" ;;
esac

(
  cd "$WORKDIR"
  rm -f "$OUT_ABS"
  zip -q -9 "$OUT_ABS" catalog.json wallpaper.*
)

echo "Wrote $OUT_ABS"
shasum -a 256 "$OUT_ABS" | tee "${OUT_ABS}.sha256"
unzip -l "$OUT_ABS"
