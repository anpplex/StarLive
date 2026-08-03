#!/usr/bin/env bash
# Offline-friendly smoke for LicenseHub starlive exchange (no car).
# Usage:
#   BASE=https://buy.998618.xyz ./scripts/test-redeem-api.sh
#   CODE=XXXX DEVICE=dev1 ./scripts/test-redeem-api.sh   # R2 re-redeem if CODE set
set -euo pipefail
BASE="${BASE:-https://buy.998618.xyz}"
BASE="${BASE%/}"

echo "== invalid code (expect ok=false) =="
curl -sS -m 15 -X POST "$BASE/api/v1/starlive/exchange" \
  -H 'Content-Type: application/json' \
  -d '{"code":"INVALIDZZ","device_id":"offline-smoke-1","app_version":"0.1.15-quality"}'
echo

if [[ -n "${CODE:-}" ]]; then
  DEV="${DEVICE:-offline-r2-device}"
  echo "== first exchange CODE=$CODE DEVICE=$DEV =="
  curl -sS -m 20 -X POST "$BASE/api/v1/starlive/exchange" \
    -H 'Content-Type: application/json' \
    -d "{\"code\":\"$CODE\",\"device_id\":\"$DEV\",\"app_version\":\"0.1.15-quality\"}"
  echo
  echo "== R2 re-exchange same device =="
  curl -sS -m 20 -X POST "$BASE/api/v1/starlive/exchange" \
    -H 'Content-Type: application/json' \
    -d "{\"code\":\"$CODE\",\"device_id\":\"$DEV\",\"app_version\":\"0.1.15-quality\"}"
  echo
  if [[ -n "${DEVICE2:-}" ]]; then
    echo "== R3 other device DEVICE2=$DEVICE2 =="
    curl -sS -m 20 -X POST "$BASE/api/v1/starlive/exchange" \
      -H 'Content-Type: application/json' \
      -d "{\"code\":\"$CODE\",\"device_id\":\"$DEVICE2\",\"app_version\":\"0.1.15-quality\"}"
    echo
  fi
fi

echo "OK smoke done (server-side R2/R4 unit tests: LicenseHub vitest starlive.redeem.test.ts)"
