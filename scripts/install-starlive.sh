#!/usr/bin/env bash
# Build (optional) and install StarLive debug APK via adb.
#
# Usage:
#   ./scripts/install-starlive.sh           # install existing APK if present, else build
#   ./scripts/install-starlive.sh --build   # always rebuild
#   ./scripts/install-starlive.sh --launch  # start MainActivity after install
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/android"
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
BUILD=0
LAUNCH=0

for arg in "$@"; do
  case "$arg" in
    --build) BUILD=1 ;;
    --launch) LAUNCH=1 ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
    *) echo "Unknown arg: $arg" >&2; exit 1 ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 1
fi

if [[ ! -f "$APK" || "$BUILD" -eq 1 ]]; then
  echo "==> Building debug APK"
  (cd "$APP" && ./gradlew :app:assembleDebug)
fi

if [[ ! -f "$APK" ]]; then
  echo "APK missing: $APK" >&2
  exit 1
fi

echo "==> Devices"
adb devices

echo "==> Installing $APK"
adb install -r "$APK"

if [[ "$LAUNCH" -eq 1 ]]; then
  echo "==> Launch MainActivity"
  adb shell am start -n com.starlive.app/.ui.MainActivity
fi

echo "done"
