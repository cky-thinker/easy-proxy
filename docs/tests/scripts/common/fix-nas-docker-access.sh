#!/usr/bin/env bash
set -euo pipefail

nas_user="${1:-chenkeyudev}"
docker_group="${2:-docker}"
config_file="/var/packages/ContainerManager/etc/dockerd.json"
socket_file="/var/run/docker.sock"
synogroup="/usr/syno/sbin/synogroup"

if [[ ${EUID} -ne 0 ]]; then
  echo "this script must run as root" >&2
  exit 1
fi
if [[ ! -f "$config_file" ]]; then
  echo "missing docker daemon config: $config_file" >&2
  exit 1
fi
if [[ ! -S "$socket_file" ]]; then
  echo "missing docker socket: $socket_file" >&2
  exit 1
fi
command -v python3 >/dev/null 2>&1 || {
  echo "python3 is required to update dockerd.json safely" >&2
  exit 1
}

echo "nas_user=$nas_user"
echo "docker_group=$docker_group"

if "$synogroup" --get "$docker_group" >/dev/null 2>&1; then
  echo "group_created=no"
else
  "$synogroup" --add "$docker_group" "$nas_user" >/dev/null
  echo "group_created=yes"
fi

if id -nG "$nas_user" | tr ' ' '\n' | grep -Fxq "$docker_group"; then
  echo "group_member_added=no"
else
  "$synogroup" --memberadd "$docker_group" "$nas_user" >/dev/null
  echo "group_member_added=yes"
fi

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_file="${config_file}.bak-easy-proxy-${timestamp}"
cp -a "$config_file" "$backup_file"
echo "config_backup=$backup_file"

python3 - "$config_file" "$docker_group" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
group = sys.argv[2]
data = json.loads(path.read_text(encoding='utf-8'))
data['group'] = group
path.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
PY
python3 -m json.tool "$config_file" >/dev/null
echo "dockerd_config_group=$docker_group"
echo "dockerd_config_valid=yes"

systemctl restart pkg-ContainerManager-dockerd.service
ready=no
for _ in $(seq 1 30); do
  if [[ -S "$socket_file" ]] && /usr/local/bin/docker info >/dev/null 2>&1; then
    ready=yes
    break
  fi
  sleep 2
done
if [[ "$ready" != yes ]]; then
  echo "docker_daemon_restart=failed" >&2
  exit 1
fi
echo "docker_daemon_restart=ok"

chgrp "$docker_group" "$socket_file"
chmod 0660 "$socket_file"
echo "socket_owner=$(stat -c '%U:%G' "$socket_file")"
echo "socket_mode=$(stat -c '%a' "$socket_file")"

user_groups="$(id -nG "$nas_user")"
echo "user_groups=$user_groups"
if ! printf '%s\n' "$user_groups" | tr ' ' '\n' | grep -Fxq "$docker_group"; then
  echo "docker_group_membership=missing" >&2
  exit 1
fi
echo "docker_group_membership=yes"

if sudo -u "$nas_user" -H /usr/local/bin/docker info >/dev/null 2>&1; then
  echo "docker_access_as_user=yes"
  echo "docker_server=$(sudo -u "$nas_user" -H /usr/local/bin/docker version --format '{{.Server.Version}}')"
else
  echo "docker_access_as_user=no" >&2
  exit 1
fi
