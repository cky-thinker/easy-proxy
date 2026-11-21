#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 5 ]]; then
  echo "usage: $0 <registry> <namespace> <tag> <service> <push|load>" >&2
  exit 2
fi

registry=$1
namespace=$2
tag=$3
service=$4
action=$5

case "$service" in
  server|client|web) ;;
  *) echo "unsupported service: $service" >&2; exit 2 ;;
esac

case "$action" in
  push|load) ;;
  *) echo "unsupported action: $action" >&2; exit 2 ;;
esac

repo_root=${EASY_PROXY_REPO_ROOT:-/mnt/f/workspace/my/easy-proxy}
context="$repo_root/easy-proxy-$service"
image="$registry/$namespace/easy-proxy-$service:$tag"

if [[ ! -d "$context" ]]; then
  echo "build context not found: $context" >&2
  exit 2
fi

echo "service=$service"
echo "image=$image"
echo "context=$context"
echo "platform=linux/amd64"
echo "provenance=false"
echo "sbom=false"
echo "action=$action"

output_flag="--$action"

BUILDX_NO_DEFAULT_ATTESTATIONS=1 docker buildx build \
  --platform linux/amd64 \
  --provenance=false \
  --sbom=false \
  --pull=false \
  --progress=plain \
  "$output_flag" \
  --tag "$image" \
  "$context"