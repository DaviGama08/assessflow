#!/usr/bin/env bash
set -euo pipefail
# Usage: APP_URL=https://app.example.com API_URL=https://api.example.com ./scripts/smoke-production.sh
APP_URL="${APP_URL:?set APP_URL}"
API_URL="${API_URL:?set API_URL}"
curl -fsS "${APP_URL}" >/dev/null
curl -fsS "${API_URL}/actuator/health/liveness"
curl -fsS "${API_URL}/actuator/health/readiness"
echo
echo "Frontend and API health endpoints responded over HTTPS."
echo "Complete login, refresh, live join, answer and finish in a browser before calling the release done."
