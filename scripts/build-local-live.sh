#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/web"
npm ci
VITE_SAME_ORIGIN=true npm run build
rm -rf "$ROOT/api/src/main/resources/static"
mkdir -p "$ROOT/api/src/main/resources/static"
cp -a "$ROOT/web/dist/." "$ROOT/api/src/main/resources/static/"
cd "$ROOT/api"
./mvnw -q -DskipTests package
echo "Local Live jar: $ROOT/api/target/assessflow-api-0.1.0.jar"
echo "Run: SPRING_PROFILES_ACTIVE=local-live java -jar $ROOT/api/target/assessflow-api-0.1.0.jar"
