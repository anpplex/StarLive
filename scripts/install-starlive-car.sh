#!/usr/bin/env bash
# Install StarLive on Avatr/Huawei car HU (same bypass as Lyra install-lyra-car.sh).
# Plain `adb install` fails with INSTALL_FAILED_INTERNAL_ERROR (Hw verifyApp -3).
#
# Usage:
#   ./scripts/install-starlive-car.sh [serial] [apk]
#   SERIAL=LD249H019625 ./scripts/install-starlive-car.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SER="${1:-${SERIAL:-LD249H019625}}"
APK="${2:-$ROOT/android/app/build/outputs/apk/debug/app-debug.apk}"
REMOTE=/data/local/tmp/starlive.apk
PKG=com.starlive.app
ADB="${ADB:-adb}"

if [[ ! -f "$APK" ]]; then
  echo "APK missing: $APK" >&2
  echo "Build: cd $ROOT/android && ./gradlew :app:assembleDebug" >&2
  exit 1
fi

cleanup() {
  "$ADB" -s "$SER" shell pm enable --user 12 com.android.packageinstaller >/dev/null 2>&1 || true
  "$ADB" -s "$SER" shell pm enable --user 0 com.android.packageinstaller >/dev/null 2>&1 || true
  "$ADB" -s "$SER" shell rm -f "$REMOTE" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "=== install StarLive on $SER ==="
"$ADB" -s "$SER" push "$APK" "$REMOTE"
"$ADB" -s "$SER" shell pm disable-user --user 12 com.android.packageinstaller
"$ADB" -s "$SER" shell pm disable-user --user 0 com.android.packageinstaller || true
"$ADB" -s "$SER" shell pm install -r -d -g -t -i com.huawei.appinstaller.car --user 12 "$REMOTE"
"$ADB" -s "$SER" shell pm install -r -d -g -t -i com.huawei.appinstaller.car --user 0 "$REMOTE" || true
"$ADB" -s "$SER" shell pm enable --user 12 "$PKG" || true
"$ADB" -s "$SER" shell am start --user 12 -n "$PKG/.ui.MainActivity" || true
"$ADB" -s "$SER" shell pm path --user 12 "$PKG"
"$ADB" -s "$SER" shell dumpsys package "$PKG" | grep -E 'versionName|versionCode|installerPackageName' | head -8
echo "OK: $PKG (installer=com.huawei.appinstaller.car)"
