# AssessFlow

AssessFlow is a modern assessment platform built with Spring Boot, React and PostgreSQL. It provides a web foundation for creating and managing assessments for training, education, recruitment and interactive evaluation workflows.

AssessFlow evolved from the academic **Distributed Quiz Platform**, which explored distributed systems, networking, replication and failover. This professional version is a separate, web-oriented codebase with its own Git history. The academic project at `isec/distributed-quiz` remains unchanged and serves as a domain reference.

## Current scope

Phase 1 provides Assessment CRUD through a REST API and a responsive web interface. Assessments can be created, listed, viewed, edited and deleted. The application uses Flyway for schema migrations, Hibernate schema validation, and PostgreSQL persistence. See [architecture](docs/architecture.md), [academic architecture](docs/legacy-architecture.md), and [ADR 0001](docs/adr/0001-modular-monolith.md).

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

The default database and local user are `assessflow` and `assessflow_local`. Renaming the repository folder changes the default Docker Compose project name, so Compose normally creates a new named volume and leaves the old `distributed-quiz` volume untouched. If you set a fixed Compose project name or reuse an existing PostgreSQL volume, its database and user may still have the old names. In that case, create the new database and user or deliberately recreate your disposable local volume after checking its contents. Do not remove a volume with data you need. Flyway runs the existing `V1__create_assessments.sql` migration on an empty database; Hibernate validates the schema at startup.

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

CI runs backend tests and frontend formatting, lint and build checks on `main` and `dev` pushes and pull requests.

## Roadmap

- Phase 1 — Modern web foundation ✅
- Phase 1.5 — AssessFlow product identity ✅
- Phase 2 — Organizations, identity, multi-tenancy and Question Bank
- Phase 3 — Live Sessions, WebSocket and QR Code
- Phase 4 — Local Live Mode, resilience and offline capabilities
- Phase 5 — Redis, messaging, observability and production scaling

The later phases are plans, not current capabilities.
