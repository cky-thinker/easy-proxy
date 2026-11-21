#!/usr/bin/env bash
set -euo pipefail

output=${1:-docs/tests/runs/$(date +%Y%m%d-%H%M%S)/raw/validation/acr-logout.txt}
mkdir -p "$(dirname "$output")"

if grep -q 'registry.cn-hangzhou.aliyuncs.com' "$HOME/.docker/config.json" 2>/dev/null; then
  entry=yes
else
  entry=no
fi

{
  printf 'checked_at=%s\n' "$(date -Iseconds)"
  printf 'ACR_AUTH_ENTRY=%s\n' "$entry"
} | tee "$output"

test "$entry" = no