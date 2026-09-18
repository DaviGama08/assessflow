# AssessFlow

AssessFlow is a modern assessment platform built with Spring Boot, React and PostgreSQL. Organizations isolate tenants. Authors manage a reusable question bank and assemble assessments inside a branded workspace.

AssessFlow evolved from the academic **Distributed Quiz Platform**, which explored distributed systems, networking, replication and failover. This professional version is a separate, web-oriented codebase with its own Git history. The academic project at `isec/distributed-quiz` remains unchanged and serves as a domain reference.

## Current scope

Phase 3 adds live sessions on top of Phase 2: published assessments, join codes, QR, a waiting room, WebSocket/STOMP, live questions, answers, results and reconnect. See [architecture](docs/architecture.md), [academic architecture](docs/legacy-architecture.md), [ADR 0001](docs/adr/0001-modular-monolith.md), [ADR 0002](docs/adr/0002-authentication-strategy.md), [ADR 0003](docs/adr/0003-multi-tenancy-strategy.md), [ADR 0004](docs/adr/0004-question-bank-reuse.md) and [ADR 0005](docs/adr/0005-live-session-realtime.md).

```text
React + TypeScript + Vite
          ↓ REST /api/v1
Spring Boot 4.1.1 (Java 25)
          ↓ JPA / Hibernate + Flyway
PostgreSQL 17
```

## Requirements

- Java 25
- Node 24 LTS (`frontend/.nvmrc`)
- Docker with Compose v2

## Run locally

1. Copy `.env.example` to `.env`. Its credentials are for local development only. Keep `.env` untracked.
2. Run `docker compose up -d postgres` from the repository root. Wait for the container healthcheck. The default host port is 55432; set `POSTGRES_PORT` and the matching port in `DB_URL` to change it.
3. In another terminal, run `cd backend && ./mvnw spring-boot:run`. The default `local` profile uses the Compose database. Override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `APP_CORS_ALLOWED_ORIGINS` when needed.
4. In another terminal, run `cd frontend && npm ci && npm run dev`. Open <http://localhost:5173>. Set `VITE_API_BASE_URL` if the API is elsewhere; it defaults to `http://localhost:8080/api/v1`. Set `VITE_PUBLIC_APP_URL` to the URL encoded in join QR codes (default `http://localhost:5173`) and `VITE_WS_URL` for STOMP (default `ws://localhost:8080/ws`).
5. Check <http://localhost:8080/actuator/health> for `{"status":"UP"}`.
6. Publish an assessment, click **Start live session**, and open `/join/{code}` in another browser. Guests do not need an account.

The default database and local user are `assessflow` and `assessflow_local`. Flyway applies `V1` through `V12` on an empty database; Hibernate validates the schema at startup. `V5` deletes assessments created before organization scoping because they cannot be attributed to a tenant. `V9`–`V12` add live sessions, question snapshots, participants and answers.

## Implemented

- Authentication: register, login, refresh, logout, BCrypt passwords, hashed opaque tokens
- Organizations and memberships with OWNER, ADMIN, INSTRUCTOR and PARTICIPANT
- Last-owner protection
- Multi-tenancy through `organization_id` and scoped queries
- Organization-scoped assessment CRUD and configuration
- Reusable question bank with categories and option validation
- Assessment builder that links bank questions with points and order
- Organization branding (display name, logo URL, colors) without custom HTML/CSS/JS
- Assessment lifecycle: `DRAFT → PUBLISHED → ARCHIVED` through explicit publish/archive endpoints
- Live sessions from published assessments, with join codes, QR, waiting room and guest participants
- WebSocket/STOMP events, REST answer submission, per-question results and reconnect from persisted state

## API

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Register |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Rotate refresh token |
| POST | `/api/v1/auth/logout` | Logout |
| GET | `/api/v1/auth/me` | Current user |
| POST | `/api/v1/organizations` | Create organization |
| GET | `/api/v1/organizations` | List memberships |
| GET | `/api/v1/organizations/{organizationId}` | Organization detail |
| PATCH | `/api/v1/organizations/{organizationId}` | Rename |
| GET/POST | `/api/v1/organizations/{organizationId}/members` | List / add members |
| PATCH | `/api/v1/organizations/{organizationId}/members/{memberId}/role` | Change role |
| DELETE | `/api/v1/organizations/{organizationId}/members/{memberId}` | Remove member |
| GET | `/api/v1/organizations/{organizationId}/dashboard` | Simple counts |
| GET/PUT | `/api/v1/organizations/{organizationId}/branding` | Read / update branding |
| POST/GET | `/api/v1/organizations/{organizationId}/assessments` | Create / list |
| GET/PUT/DELETE | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}` | Detail / update / delete |
| POST/GET/DELETE | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}/questions/{questionId}` | Link / list / unlink |
| PUT | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}/questions/order` | Reorder |
| POST/GET | `/api/v1/organizations/{organizationId}/questions` | Create / list questions |
| GET/PUT/DELETE | `/api/v1/organizations/{organizationId}/questions/{questionId}` | Detail / update / archive |
| POST/GET | `/api/v1/organizations/{organizationId}/question-categories` | Create / list categories |
| POST | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}/publish` | Publish a draft |
| POST | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}/archive` | Archive |
| POST | `/api/v1/organizations/{organizationId}/assessments/{assessmentId}/live-sessions` | Create a live session |
| GET | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}` | Host session detail |
| POST | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}/start` | Start (first question) |
| POST | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}/questions/end` | Close current question |
| POST | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}/questions/next` | Open the next question |
| POST | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}/finish` | Finish |
| POST | `/api/v1/organizations/{organizationId}/live-sessions/{sessionId}/cancel` | Cancel |
| GET | `/api/v1/live-sessions/preview?code=` | Public session preview |
| POST | `/api/v1/live-sessions/join` | Guest join |
| GET | `/api/v1/live-sessions/{sessionId}/state` | Participant reconnect state |
| POST | `/api/v1/live-sessions/{sessionId}/answers` | Submit an answer |
| WS | `/ws` | STOMP, topic `/topic/sessions/{sessionId}` |

Invalid requests return 400 and missing tenant resources return 404 using ProblemDetail. Page size is capped at 100. Guest join does not require an AssessFlow account.

## Quality checks

```bash
cd backend && ./mvnw clean verify
cd frontend && npm ci && npm run format:check && npm run lint && npm test && npm run build
# From the root:
docker compose config
```

CI runs backend tests and frontend formatting, lint, tests and build checks on `main` and `dev` pushes and pull requests.

## Roadmap

- Phase 1 — Modern web foundation ✅
- Phase 1.5 — AssessFlow product identity ✅
- Phase 2 — Organizations, identity, multi-tenancy, Question Bank, Assessment Builder and branding ✅
- Phase 3 — Live Sessions, WebSocket, join codes and QR Code ✅
- Phase 4 — Local Live Mode, LAN join QR, assessment packages, Ubuntu hotspot helper ✅
- Phase 5 — Redis, messaging, observability and production scaling

Local Live works without Internet, but participating devices must share a local network. See [Local Live](docs/local-live.md) and [ADR 0006](docs/adr/0006-local-live-mode.md). The STOMP simple broker remains single-instance. Redis, RabbitMQ, Kafka and automatic cloud sync are not implemented.
