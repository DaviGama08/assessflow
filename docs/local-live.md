# AssessFlow Local Live

Local Live runs a live session **without Internet**. Phones still need a local network (same Wi-Fi or the laptop hotspot).

## Easy mode — same Wi-Fi

1. On a network, run `scripts/build-local-live.sh` and `scripts/local-live/prepare.sh`.
2. Start PostgreSQL with `docker compose -f compose.local-live.yaml up -d` (port bound to 127.0.0.1).
3. `SPRING_PROFILES_ACTIVE=local-live java -jar backend/target/assessflow-backend-0.1.0.jar`
4. Open `http://{LAN-IP}:8080`, sign in (local account), import a `.assessflow.json` package or use a local published assessment, start a live session.
5. Disconnect the Internet uplink if you want. Keep the LAN.
6. Phones on the same Wi-Fi scan the Join QR.

## Advanced mode — Ubuntu hotspot

1. Host clicks **Start Local Hotspot** (or `scripts/local-live/hotspot.sh start`).
2. Scan the **Wi-Fi QR**.
3. If the OS opens a captive portal, it should land on `/join`. That is best effort and not guaranteed on Android/iOS.
4. If the portal does not open, scan the **Join QR**.
5. Stop the hotspot from the UI or `hotspot.sh stop` (SIGINT/SIGTERM also stop it).

Windows and macOS: use same-LAN Local Live or the OS manual hotspot. Automatic `nmcli` support is Linux/Ubuntu only.

## Security

- PostgreSQL is not on the LAN.
- Host start/next/finish still need a logged-in OWNER/ADMIN/INSTRUCTOR.
- Participant tokens expire (`APP_LIVE_PARTICIPANT_TOKEN_TTL`, default 12h).
- Local HTTP is accepted because phone trust of a laptop certificate is worse; the WPA hotspot and join code are the LAN boundary.
