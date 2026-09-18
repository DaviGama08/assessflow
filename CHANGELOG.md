# Changelog

## 1.0.0 — Phase 6

Multi-tenant assessments, question bank, live sessions, Local Live without Internet, optional Redis/RabbitMQ scale, and production deployment assets (container image, GHCR, Azure Container Apps, Cloudflare Pages, Neon).

### Added
- Production fail-fast validation for Redis, RabbitMQ relay, DB TLS, CORS and replica/broker pairing
- Configurable refresh-cookie SameSite (None requires Secure)
- Explicit HTTP proxy modes: `none`, `forwarded`, `cloudflare`
- OpenAPI (`/v3/api-docs`) on local/test; Swagger UI off in production
- Multi-stage non-root backend image and GHCR publish workflow
- Dependabot, CodeQL, Trivy + CycloneDX SBOM on the published image
- Playwright live-session E2E in CI
- Deployment runbook and production checklist

### Changed
- Production profile defaults to lean mode (simple broker, Redis off)
- README describes completed Phases 1–6 without claiming unimplemented brokers
- Embedded Tomcat pinned to 11.0.26 (Spring Boot 4.1.1 still manages 11.0.24)
- Client STOMP `SEND` rejected on the inbound channel (commands stay on REST)
- Azure deploys the Trivy-scanned image digest; GHCR pull is anonymous for the public package
