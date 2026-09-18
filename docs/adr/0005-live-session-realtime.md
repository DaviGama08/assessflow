# ADR 0005: Live session transport and identity

**Status:** Accepted

## Context

Phase 3 turns a published assessment into a live session. Instructors host from the workspace. Guests join with a short code or QR without an AssessFlow account. Question-bank rows stay mutable (ADR 0004), so a session must freeze the content it uses. The product is still a single modular monolith. Redis, brokers and multi-instance fan-out are Phase 5.

## Decision

**Commands stay on REST. Notifications use WebSocket/STOMP.** Creating a session, joining, starting, closing a question, moving to the next question, submitting an answer, finishing and cancelling are HTTP commands. Spring WebSocket with STOMP and an in-memory simple broker push typed events to `/topic/sessions/{sessionId}`:

`PARTICIPANT_JOINED`, `PARTICIPANT_LEFT`, `SESSION_STARTED`, `QUESTION_STARTED`, `ANSWER_RECEIVED`, `QUESTION_ENDED`, `QUESTION_RESULTS`, `SESSION_FINISHED`, `SESSION_CANCELLED`.

**PostgreSQL is the source of truth.** Session status, current question, snapshots, participants, tokens and answers are persisted. A client that misses an event recovers with `GET /api/v1/live-sessions/{sessionId}/state` (participant) or the host GET endpoints. WebSocket memory is not authoritative.

**Join codes** are six characters from `ABCDEFGHJKMNPQRSTUVWXYZ23456789`, stored uppercase, compared case-insensitively, and unique in `live_sessions`. Collisions retry with a new code.

**Guest identity** is an opaque 32-byte hex token, stored as SHA-256. The browser keeps the raw token in `sessionStorage`. REST and STOMP CONNECT send `Authorization: Bearer <token>`. The filter binds the token to one participant and one session. The client cannot pick a `participantId`.

**Snapshots.** `LiveSessionQuestion` and `LiveSessionQuestionOption` copy text, type, order, points and correctness at create time. Participant question payloads never include `correct`. Shared STOMP `QUESTION_RESULTS` also omit `correct`; the host reads correctness from REST.

**Concurrency.** Answers have `UNIQUE (live_session_id, live_session_question_id, participant_id)`. Duplicate submits return 409. `LiveSession` uses `@Version`. Invalid transitions live on the entity (`WAITING → ACTIVE → FINISHED`, cancel from waiting or active).

**Reconnect.** Refreshing the participant page reloads state with the stored token and resubscribes. The host reloads REST state and resubscribes with the user access token.

**Single-instance limitation.** The simple broker only notifies connections on the same process. Horizontal scale needs an external broker (documented for Phase 5). Local Live Mode (LAN URL, hotspot, captive portal) is not part of this decision.

## Consequences

Live sessions work on one JVM with PostgreSQL. Guests can join from a phone by scanning a QR that contains only `{PUBLIC_APP_URL}/join/{code}`. Instructors cannot start a session from a DRAFT assessment. Future Local Live Mode can swap the public base URL without changing domain logic.
