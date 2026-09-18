# ADR 0006: Local Live Mode

**Status:** Accepted

## Context

Phase 3 live sessions need a network between host and phones, but they should not need the public Internet. Cloud URLs, Vite, and wildcard WebSocket origins do not work on a laptop hotspot or an isolated LAN.

## Decision

Local Live is the same modular monolith with profile `local-live`. Spring Boot binds `0.0.0.0`, serves the React production build from `/`, and exposes API `/api/v1` and STOMP `/ws` on that origin. PostgreSQL stays on `127.0.0.1`. LAN addresses are discovered at runtime (RFC1918, skipping loopback/Docker/VPN). `LOCAL_LIVE_HOST` overrides the advertised IP. Join QR is `http://{host}:{port}/join/{code}`.

Guest tokens remain short-lived. Host commands still require an AssessFlow user in the local database. Published assessments travel as a JSON Local Event Package (`schemaVersion` 1 + SHA-256). Import remaps IDs into a `Local Events` organization. Session results export as CSV. Cloud sync is out of scope.

Linux/Ubuntu can start a NetworkManager hotspot via `scripts/local-live/hotspot.sh` after an explicit host action. Windows/macOS use same-LAN or a manual hotspot. Captive-portal probes redirect to `/join` as best effort; the OS decides whether a portal opens. HTTP/WS is used because trusted local HTTPS on phones is worse.

## Consequences

An event can run with no Internet if Java, the jar, PostgreSQL and (optionally) the Postgres image were prepared in advance. Automatic hotspot and captive portal are not guaranteed. The simple STOMP broker remains single-instance.
