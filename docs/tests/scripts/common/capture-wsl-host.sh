#!/usr/bin/env bash
set -euo pipefail

repo_root=${1:-/mnt/f/workspace/my/easy-proxy}
output=${2:-docs/tests/runs/$(date +%Y%m%d-%H%M%S)/raw/validation/local-wsl-build-host.txt}
cd "$repo_root"
mkdir -p "$(dirname "$output")"

{
  printf 'captured_at=%s\n' "$(date -Iseconds)"
  printf 'wsl_distro=%s\n' "${WSL_DISTRO_NAME:-unknown}"
  printf 'kernel=%s\n' "$(uname -srmo)"
  printf 'os_release=%s\n' "$(. /etc/os-release && printf '%s' "$PRETTY_NAME")"
  printf 'project_path=%s\n' "$repo_root"
  printf 'daemon_arch=%s\n' "$(docker info --format '{{.Architecture}}' 2>/dev/null || printf 'unavailable')"
  printf 'docker_client=%s\n' "$(docker version --format '{{.Client.Version}}' 2>/dev/null || printf 'unavailable')"
  printf 'docker_server=%s\n' "$(docker version --format '{{.Server.Version}}' 2>/dev/null || printf 'unavailable')"
  printf 'buildx=%s\n' "$(docker buildx version 2>/dev/null || printf 'unavailable')"
  printf 'compose=%s\n' "$(docker compose version 2>/dev/null || printf 'unavailable')"
  if command -v java >/dev/null 2>&1; then
    printf 'java=%s\n' "$(java -version 2>&1 | head -n 1)"
  else
    printf 'java=unavailable\n'
  fi
  if mvn -version >/dev/null 2>&1; then
    printf 'maven=%s\n' "$(mvn -version 2>&1 | head -n 1)"
  else
    printf 'maven=unavailable_in_wsl\n'
  fi
  printf 'windows_maven_used_for_build=yes\n'
  printf 'source_traceability=windows-git\n'
} > "$output"

cat "$output"