#!/usr/bin/env bash
# Tag + GitHub Release + attach APK (debug or release).
# Usage:
#   ./scripts/publish-github-release.sh v0.1.15-quality
#   APK=path/to.apk ./scripts/publish-github-release.sh v0.1.15-quality
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${1:-}"
if [[ -z "$TAG" ]]; then
  echo "usage: $0 <tag>  e.g. v0.1.15-quality" >&2
  exit 1
fi

APK="${APK:-}"
if [[ -z "$APK" ]]; then
  for c in \
    "$ROOT/android/app/build/outputs/apk/release/app-release.apk" \
    "$ROOT/android/app/build/outputs/apk/release/app-release-unsigned.apk" \
    "$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
  do
    if [[ -f "$c" ]]; then APK="$c"; break; fi
  done
fi
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "No APK found. Build first: ./scripts/build-release.sh or assembleDebug" >&2
  exit 1
fi

cd "$ROOT"
if ! git rev-parse "$TAG" >/dev/null 2>&1; then
  git tag -a "$TAG" -m "$TAG"
  git push origin "$TAG"
fi

NOTES="See CHANGELOG.md for $TAG"
if gh release view "$TAG" >/dev/null 2>&1; then
  gh release upload "$TAG" "$APK" --clobber
  echo "Uploaded $APK to existing release $TAG"
else
  gh release create "$TAG" "$APK" --title "$TAG" --notes "$NOTES"
  echo "Created release $TAG with $APK"
fi
