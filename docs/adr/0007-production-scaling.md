# ADR 0007: Production scaling without splitting the monolith

**Status:** Accepted

## Context

Phase 4 proved Local Live on one JVM with PostgreSQL and a simple STOMP broker. Cloud events need more than one interchangeable API process so a participant on instance A still receives a host command handled on instance B. Redis, RabbitMQ, metrics and tracing are tools for that goal, not a microservices redesign.

## Decision

AssessFlow stays a modular monolith. PostgreSQL remains the durable source of truth for sessions, answers and identity.

**Realtime.** `app.realtime.broker-mode` is `simple` (in-process STOMP broker) or `relay` (RabbitMQ STOMP on port 61613). `local`, `test` and `local-live` use `simple`. The `production` profile uses `relay`. Invalid values fail startup. `guest/guest` is rejected. Public topic names stay `/topic/sessions/{sessionId}` and `/topic/host/sessions/{sessionId}`. Relay mode maps those paths onto `/exchange/amq.topic` routing keys because RabbitMQ STOMP rejects extra slashes in `/topic/...` destinations. CONNECT authentication and SUBSCRIBE authorization stay in the application.

**After commit.** Domain methods still run in a transaction. STOMP publish happens in an `AFTER_COMMIT` listener. A broker failure is logged and counted; it does not roll back answers or session state. Clients recover from REST.

**Redis.** Optional (`app.redis.enabled`, default false). Used only for distributed rate limiting of public join, preview, login and register. Keys are `assessflow:rate:{bucket}:{sha256-prefix}` — never email, name or raw join codes. The Lua increment+expire+limit check is atomic. Redis errors fail open with a warning and a metric. `X-Forwarded-For` is ignored unless `app.http.trusted-proxy=true` behind a proxy that overwrites forwarded headers (`server.forward-headers-strategy` stays `none` by default).

**Observability.** Micrometer counters use low-cardinality tags (`outcome` on rate-limit decisions). Prometheus scrape is opt-in on the production profile. OpenTelemetry tracing is off unless `OTEL_TRACES_ENABLED=true` and an OTLP endpoint is set. Production logs use ECS JSON. Liveness is process health; readiness is database (and STOMP relay in `relay` mode). Redis is not a readiness dependency because rate limiting fails open.

**Process.** Graceful Tomcat shutdown and configurable Hikari pool size. Local Live does not start Redis, RabbitMQ, Prometheus or an OTLP collector.

## Consequences

Two API instances behind a load balancer can run the same live session. Operators opt into Redis and RabbitMQ. A laptop event still runs Spring Boot + PostgreSQL + simple STOMP.
