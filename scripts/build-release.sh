#!/usr/bin/env bash
# Build signed release APK when android/keystore.properties exists; else unsigned release.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android"

if [[ ! -f keystore.properties ]]; then
  echo "WARN: android/keystore.properties missing — release will be unsigned."
  echo "      See android/keystore.properties.example"
fi

./gradlew :app:assembleRelease --stacktrace
APK="app/build/outputs/apk/release/app-release.apk"
UNSIGNED="app/build/outputs/apk/release/app-release-unsigned.apk"
if [[ -f "$APK" ]]; then
  echo "OK: $ROOT/android/$APK"
  ls -la "$APK"
elif [[ -f "$UNSIGNED" ]]; then
  echo "OK (unsigned): $ROOT/android/$UNSIGNED"
  ls -la "$UNSIGNED"
else
  echo "FAIL: no release APK found" >&2
  exit 1
fi
