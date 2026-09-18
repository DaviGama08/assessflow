#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/frontend"
npm ci
VITE_SAME_ORIGIN=true npm run build
rm -rf "$ROOT/backend/src/main/resources/static"
mkdir -p "$ROOT/backend/src/main/resources/static"
cp -a "$ROOT/frontend/dist/." "$ROOT/backend/src/main/resources/static/"
cd "$ROOT/backend"
./mvnw -q -DskipTests package
echo "Local Live jar: $ROOT/backend/target/assessflow-backend-0.1.0.jar"
echo "Run: SPRING_PROFILES_ACTIVE=local-live java -jar $ROOT/backend/target/assessflow-backend-0.1.0.jar"
