# ADR 0001: Start with a modular monolith

**Status:** Accepted

## Context

Phase 1 has one small domain and a single PostgreSQL database. The academic system used several server processes to demonstrate failover, but the web product has no independently deployable domain boundaries yet.

## Decision

Keep one Spring Boot application, organized by feature with API, application, domain and infrastructure packages. The frontend remains a separate application. Use stable REST contracts and Flyway migrations.

## Consequences

Development, transactions, testing and local setup stay simple. Future features can be added as neighboring modules, with Spring Modulith possible later. Scaling, live transport and service extraction require separate decisions when real use cases justify them.
