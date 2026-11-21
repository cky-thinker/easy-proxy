#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <image-reference> <text-output> <json-output>" >&2
  exit 2
fi

image=$1
text_output=$2
json_output=$3

mkdir -p "$(dirname "$text_output")" "$(dirname "$json_output")"

docker buildx imagetools inspect "$image" | tee "$text_output"
docker buildx imagetools inspect "$image" --format '{{json .}}' > "$json_output"