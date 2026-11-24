#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."

jar_path=api/target/assessflow-api-0.1.0.jar
if [[ ! -f "$jar_path" ]]; then
  echo "Build the application with scripts/build-local-live.sh first." >&2
  exit 1
fi

# Select the LAN interface used by the default route, excluding Docker bridges.
lan_host=${LOCAL_LIVE_HOST:-}
if [[ -z "$lan_host" ]] && command -v ip >/dev/null; then
  lan_iface=$(ip -4 route show default | awk 'NR == 1 {for (i=1;i<NF;i++) if ($i=="dev") print $(i+1)}')
  if [[ -n "$lan_iface" ]]; then
    lan_host=$(ip -4 -o address show dev "$lan_iface" scope global | awk 'NR == 1 {split($4,a,"/"); print a[1]}')
  fi
fi
export LOCAL_LIVE_HOST="${lan_host:-127.0.0.1}"
docker compose -f compose.yaml up -d --wait postgres
echo "Teacher console: http://localhost:${LOCAL_LIVE_PORT:-8080}"
exec java -jar "$jar_path" --spring.profiles.active=local-live \
  '--spring.config.import=optional:file:./.env[.properties]'
