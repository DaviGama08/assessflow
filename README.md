# AssessFlow

AssessFlow is a modern assessment platform built with Spring Boot, React and PostgreSQL. Organizations isolate tenants. Authors manage a reusable question bank and assemble assessments inside a branded workspace.

AssessFlow evolved from the academic **Distributed Quiz Platform**, which explored distributed systems, networking, replication and failover. This professional version is a separate, web-oriented codebase with its own Git history. The academic project at `isec/distributed-quiz` remains unchanged and serves as a domain reference.

## Current scope

Phase 2 provides authentication, organizations, role-based access, multi-tenancy, a question bank, an assessment builder and organization branding. See [architecture](docs/architecture.md), [academic architecture](docs/legacy-architecture.md), [ADR 0001](docs/adr/0001-modular-monolith.md), [ADR 0002](docs/adr/0002-authentication-strategy.md), [ADR 0003](docs/adr/0003-multi-tenancy-strategy.md) and [ADR 0004](docs/adr/0004-question-bank-reuse.md).

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
4. In another terminal, run `cd frontend && npm ci && npm run dev`. Open <http://localhost:5173>. Set `VITE_API_BASE_URL` if the API is elsewhere; it defaults to `http://localhost:8080/api/v1`.
5. Check <http://localhost:8080/actuator/health> for `{"status":"UP"}`.

The default database and local user are `assessflow` and `assessflow_local`. Flyway applies `V1` through `V8` on an empty database; Hibernate validates the schema at startup. `V5` deletes assessments created before organization scoping because they cannot be attributed to a tenant.

## Implemented

- Authentication: register, login, refresh, logout, BCrypt passwords, hashed opaque tokens
- Organizations and memberships with OWNER, ADMIN, INSTRUCTOR and PARTICIPANT
- Last-owner protection
- Multi-tenancy through `organization_id` and scoped queries
- Organization-scoped assessment CRUD and configuration
- Reusable question bank with categories and option validation
- Assessment builder that links bank questions with points and order
- Organization branding (display name, logo URL, colors) without custom HTML/CSS/JS

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

Invalid requests return 400 and missing tenant resources return 404 using ProblemDetail. Page size is capped at 100.

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
- Phase 3 — Live Sessions, WebSocket, join codes and QR Code
- Phase 4 — Local Live Mode, resilience and offline capabilities
- Phase 5 — Redis, messaging, observability and production scaling

Phase 3 and later are plans, not current capabilities. Live sessions, WebSocket, join codes, QR codes, Redis and RabbitMQ are not implemented.
