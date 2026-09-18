# Phase 1 architecture

The frontend is a React/TypeScript SPA built by Vite. Assessment views use a feature folder; HTTP calls go through a single shared wrapper and feature API module. The backend exposes versioned JSON REST endpoints. Its Assessment controller handles HTTP, the application service owns use cases and transactions, the domain entity owns state changes, and Spring Data JPA persists to PostgreSQL. DTO records keep JPA entities out of API responses.

Flyway is the only schema writer. `V1__create_assessments.sql` creates the `uuid` key, required fields, size limits, and status check. Hibernate runs with `ddl-auto=validate`; a schema mismatch fails startup. Timestamps are `Instant` values from a UTC clock and map to PostgreSQL `timestamptz`.

The API returns bounded pages sorted by creation time and UUID. Errors use RFC 9457 ProblemDetail with a stable `code`. CORS allows configured development origins only. Actuator exposes health alone; database health contributes to the overall status.

The single deployable backend is intentionally modular by feature. New domains can become neighboring packages without changing the Assessment contract. No background broker, socket protocol, or distributed database mechanism exists in this phase.
