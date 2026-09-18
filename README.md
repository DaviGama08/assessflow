# Distributed Quiz Platform

A modern web foundation for the academic Distributed Quiz Platform. This is a new codebase and Git history. Phase 1 implements assessment management only; the academic project remains a reference for domain rules.

## Architecture

```text
React + TypeScript + Vite
          ↓ REST /api/v1
Spring Boot 4.1.1 (Java 25)
          ↓ JPA / Hibernate + Flyway
PostgreSQL 17
```

The monorepo has `frontend/`, `backend/`, and `docs/`. Backend packages follow the Assessment feature, with separate API, application, domain, and infrastructure code. See [architecture](docs/architecture.md), [legacy architecture](docs/legacy-architecture.md), and [ADR 0001](docs/adr/0001-modular-monolith.md).

## Requirements

- Java 25
- Node 24 LTS (`frontend/.nvmrc`)
- Docker with Compose v2

## Run locally

1. Copy `.env.example` to `.env`. Its credentials are for local development only. Keep `.env` untracked.
2. Run `docker compose up -d postgres` from the repository root. Wait for the container healthcheck. The default host port is 55432 to avoid a local PostgreSQL conflict; set `POSTGRES_PORT` and the matching port in `DB_URL` to change it.
3. In another terminal, run `cd backend && ./mvnw spring-boot:run`. The defaults match Compose; override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `APP_CORS_ALLOWED_ORIGINS` when needed.
4. In another terminal, run `cd frontend && npm ci && npm run dev`. Open <http://localhost:5173>. Set `VITE_API_BASE_URL` if the API is elsewhere; it defaults to `http://localhost:8080/api/v1`.
5. Check <http://localhost:8080/actuator/health> for `{"status":"UP"}`.

The PostgreSQL named volume keeps data across backend and container restarts. Flyway creates the schema on an empty database; Hibernate validates it at startup.

## API

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/assessments` | Create draft, 201 + Location |
| GET | `/api/v1/assessments?page=0&size=20` | Paginated list, 200 |
| GET | `/api/v1/assessments/{id}` | Detail, 200 |
| PUT | `/api/v1/assessments/{id}` | Edit title and description, 200 |
| DELETE | `/api/v1/assessments/{id}` | Delete, 204 |

Invalid requests return 400 and missing assessments return 404 using ProblemDetail. Page size is capped at 100.

## Quality checks

```bash
cd backend && ./mvnw clean verify
cd frontend && npm ci && npm run format:check && npm run lint && npm run build
# From the root:
docker compose config
```

CI runs backend tests and frontend lint/build on `main` and `dev` pushes and pull requests.

## Roadmap

This is **Phase 1: foundation**. Future phases may add authentication, organizations, a question bank, live sessions, WebSocket updates, QR codes, local live mode, Redis, RabbitMQ, observability, and cloud deployment. None of these are implemented here.
