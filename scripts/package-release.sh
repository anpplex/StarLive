#!/usr/bin/env bash
# Package release APK as StarLive-<versionName>.apk (e.g. StarLive-0.1.41.apk)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_KTS="$ROOT/android/app/build.gradle.kts"
OUT_DIR="$ROOT/releases"
SRC="$ROOT/android/app/build/outputs/apk/release/app-release.apk"

vn="$(grep -E 'versionName\s*=' "$GRADLE_KTS" | head -1 | sed -n 's/.*"\([^"]*\)".*/\1/p')"
if [[ -z "${vn:-}" ]]; then
  echo "Could not read versionName from $GRADLE_KTS" >&2
  exit 1
fi
# Filename-safe (no spaces/path chars)
vn_safe="$(echo "$vn" | tr '/ ' '__')"
name="StarLive-${vn_safe}.apk"
dest="$OUT_DIR/$name"

if [[ "${1:-}" == "--build" ]]; then
  (cd "$ROOT/android" && ./gradlew :app:assembleRelease)
fi

if [[ ! -f "$SRC" ]]; then
  echo "Missing $SRC — run with --build or assembleRelease first" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
cp -f "$SRC" "$dest"
echo "packaged: $dest"
echo "name:     $name"
echo "version:  $vn"
ls -lh "$dest"
