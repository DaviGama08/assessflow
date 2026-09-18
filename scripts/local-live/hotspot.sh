#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-}"
SSID="${2:-AssessFlow-LIVE}"
PASSWORD="${3:-}"
STATE_DIR="${XDG_RUNTIME_DIR:-/tmp}/assessflow-hotspot"
mkdir -p "$STATE_DIR"

if [[ "${ACTION}" == "dry-run" ]]; then
  if command -v nmcli >/dev/null 2>&1; then
    echo "nmcli available"
    exit 0
  fi
  echo "nmcli unavailable"
  exit 1
fi

if [[ "${ACTION}" == "stop" ]]; then
  if [[ -f "${STATE_DIR}/connection" ]]; then
    CONN="$(cat "${STATE_DIR}/connection")"
    nmcli connection down "$CONN" >/dev/null 2>&1 || true
    nmcli connection delete "$CONN" >/dev/null 2>&1 || true
    rm -f "${STATE_DIR}/connection"
  fi
  exit 0
fi

if [[ "${ACTION}" != "start" ]]; then
  echo "usage: hotspot.sh start <ssid> <password>|stop|dry-run" >&2
  exit 1
fi

if ! command -v nmcli >/dev/null 2>&1; then
  echo "nmcli unavailable" >&2
  exit 1
fi

CONN="assessflow-hotspot"
WIFI_IFACE="${WIFI_IFACE:-}"
if [[ -z "${WIFI_IFACE}" ]]; then
  WIFI_IFACE="$(nmcli -t -f DEVICE,TYPE device status 2>/dev/null | awk -F: '$2=="wifi"{print $1; exit}')"
fi
if [[ -z "${WIFI_IFACE}" ]]; then
  echo "No Wi-Fi interface found. Set WIFI_IFACE or connect a wireless adapter." >&2
  exit 1
fi
nmcli connection delete "$CONN" >/dev/null 2>&1 || true
nmcli device wifi hotspot ifname "$WIFI_IFACE" con-name "$CONN" ssid "$SSID" password "$PASSWORD"
echo "$CONN" >"${STATE_DIR}/connection"
echo "started $SSID"
