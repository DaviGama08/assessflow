# AssessFlow WebSocket / STOMP

REST commands change live-session state. STOMP at `/ws` notifies connected hosts and guests after the database transaction commits.

## Endpoint

```text
ws://localhost:8080/ws
wss://api.<domain>/ws
```

CONNECT with `Authorization: Bearer <access-token-or-participant-token>`. Unauthenticated CONNECT is rejected.

Clients never publish. Inbound `SEND` is rejected for every principal (host included). Host and guest commands stay on REST; STOMP is notifications only. `CONNECT`, `SUBSCRIBE`, `UNSUBSCRIBE`, `DISCONNECT` and heartbeats remain allowed.

## Topics

| Destination | Who | Events |
| --- | --- | --- |
| `/topic/sessions/{sessionId}` | guest participant of that session | `SESSION_STARTED`, `QUESTION_STARTED`, sanitized `QUESTION_RESULTS`, `SESSION_FINISHED`, `SESSION_CANCELLED` |
| `/topic/host/sessions/{sessionId}` | OWNER / ADMIN / INSTRUCTOR of the organization | `PARTICIPANT_JOINED`, `PARTICIPANT_LEFT`, `PRESENCE_CHANGED`, `ANSWER_RECEIVED`, plus the session events |

A guest cannot subscribe to the host topic. Origins come from `APP_WS_ALLOWED_ORIGINS` or `APP_CORS_ALLOWED_ORIGINS`.

## Payload

Each message is JSON `{ "type": "QUESTION_STARTED", "payload": { ... } }`. Participant `QUESTION_RESULTS` omit correctness. Hosts read correctness from REST.

## Reconnect

Missed events are not a source of truth. Reload `GET /api/v1/live-sessions/{sessionId}/state` (guest) or the host GET endpoints, then subscribe again.

## Scale

`app.realtime.broker-mode=simple` is in-process (one API replica). `relay` uses RabbitMQ STOMP (port 61613). Public topic names stay the same; relay mode maps them onto `/exchange/amq.topic` routing keys internally.
