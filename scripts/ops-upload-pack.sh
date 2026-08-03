#!/usr/bin/env bash
# Upload a theme pack zip to LicenseHub VPS data volume and print register hints.
#
# Usage:
#   export LICENSEHUB_VPS=root@98.126.31.173
#   ./scripts/ops-upload-pack.sh ./dist/pack_client.zip [remote_name.zip]
#
# After upload: Admin → 星澜兑换 → 登记主题包 → 发码
set -euo pipefail

HOST="${LICENSEHUB_VPS:-root@98.126.31.173}"
LOCAL="${1:-}"
REMOTE_NAME="${2:-}"

if [[ -z "$LOCAL" || ! -f "$LOCAL" ]]; then
  echo "Usage: $0 <local.zip> [remote_filename.zip]" >&2
  exit 1
fi
if [[ ! "$LOCAL" =~ \.zip$ ]]; then
  echo "Expected a .zip file" >&2
  exit 1
fi

REMOTE_NAME="${REMOTE_NAME:-$(basename "$LOCAL")}"
REMOTE_NAME="$(basename "$REMOTE_NAME")"
SHA=$(shasum -a 256 "$LOCAL" | awk '{print $1}')
BYTES=$(wc -c <"$LOCAL" | tr -d ' ')

echo "==> local $LOCAL ($BYTES bytes)"
echo "    sha256 $SHA"
echo "==> scp → ${HOST}:/tmp/${REMOTE_NAME}"
scp -o PreferredAuthentications=publickey -o PasswordAuthentication=no \
  "$LOCAL" "${HOST}:/tmp/${REMOTE_NAME}"

echo "==> docker cp → deploy-licensehub-1:/data/starlive-packs/${REMOTE_NAME}"
ssh -o PreferredAuthentications=publickey -o PasswordAuthentication=no "${HOST}" bash -s <<EOF
set -euo pipefail
docker exec deploy-licensehub-1 mkdir -p /data/starlive-packs
docker cp /tmp/${REMOTE_NAME} deploy-licensehub-1:/data/starlive-packs/${REMOTE_NAME}
docker exec deploy-licensehub-1 ls -la /data/starlive-packs/${REMOTE_NAME}
docker exec deploy-licensehub-1 sha256sum /data/starlive-packs/${REMOTE_NAME}
EOF

echo ""
echo "Next:"
echo "  1. Open https://buy.998618.xyz/admin/ → 星澜兑换"
echo "  2. 登记主题包: object_key=${REMOTE_NAME}  id=pack_...  title=..."
echo "  3. 批量生成兑换码 (max_devices=1)"
echo "  sha256 expected: $SHA"
