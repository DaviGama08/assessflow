#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
missing=0
command -v java >/dev/null || { echo "Java is missing"; missing=1; }
command -v docker >/dev/null || { echo "Docker is missing"; missing=1; }
if ! docker image inspect postgres:17.6-alpine >/dev/null 2>&1; then
  echo "postgres:17.6-alpine image is not present. Pull it before going offline."
  missing=1
fi
if [[ ! -f "$ROOT/backend/target/assessflow-backend-0.1.0.jar" ]]; then
  echo "Local Live jar is missing. Run scripts/build-local-live.sh"
  missing=1
fi
if [[ "$missing" -ne 0 ]]; then
  exit 1
fi
echo "Local Live prerequisites are ready."
