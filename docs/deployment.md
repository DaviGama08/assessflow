# Production deployment

AssessFlow cloud topology:

```text
Users → Cloudflare (DNS, TLS, CDN, WAF, WebSocket proxy)
          ├── Pages  → React (app.<domain>)
          └── API/WSS → Azure Container Apps origin (api.<domain>)
                            └── Neon PostgreSQL
                            └── Redis / RabbitMQ only in scaled mode
```

Do not commit secrets. Trusted proxy headers are safe only if the Azure origin cannot be bypassed.

## Lean portfolio (default)

One API replica. Simple STOMP broker. In-memory public rate limiter. Neon PostgreSQL.

```text
APP_REPLICAS=1
APP_REALTIME_BROKER_MODE=simple
APP_REDIS_ENABLED=false
APP_HTTP_PROXY_MODE=cloudflare   # only after origin restriction
APP_AUTH_SECURE_COOKIE=true
APP_AUTH_REFRESH_COOKIE_SAME_SITE=Strict
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
APP_WS_ALLOWED_ORIGINS=https://app.example.com
DB_URL=jdbc:postgresql://...neon.../assessflow?sslmode=require
```

`min replicas = 0` is cheaper and can cold-start. Prefer `min replicas = 1` for a live-session demo.

## Scaled

Two or more API replicas **must** set:

```text
APP_REPLICAS=2
APP_REALTIME_BROKER_MODE=relay
APP_REDIS_ENABLED=true
REDIS_HOST=...
REDIS_PASSWORD=...
RABBITMQ_HOST=...
RABBITMQ_STOMP_PORT=61613
RABBITMQ_USERNAME=...
RABBITMQ_PASSWORD=...
APP_READINESS_INDICATORS=ping,readinessState,db,stompRelay
```

Production startup fails on localhost hosts, empty passwords, `guest/guest`, missing TLS on `DB_URL`, or 2+ replicas with simple broker.

Do not run Redis or RabbitMQ as disposable sidecars. Lean mode leaves them disabled. Scaled mode uses a managed service.

## GitHub → GHCR → Azure

CI on `main`/`dev` verifies tests, frontend, container smoke and Playwright. After CI succeeds on `main`, `Publish image` pushes `ghcr.io/<owner>/assessflow-backend:<full-sha>` (never deploy `latest`). There is no `workflow_dispatch` on that workflow: production images come only from a `main` commit whose CI succeeded. Trivy fails the job on applicable CRITICAL findings, then Azure (when configured) deploys the scanned digest `ghcr.io/<owner>/assessflow-backend@sha256:…`. CycloneDX SBOM is an artifact. The GHCR package is public; Azure pulls anonymously. Do not store `GITHUB_TOKEN` as a Container App registry password.

Azure authentication uses GitHub OIDC (`azure/login` federated credential). Create GitHub environment `production` with:

| Vars | Secrets |
| --- | --- |
| `AZURE_CLIENT_ID` | Container App secrets for `DB_PASSWORD`, Redis/RabbitMQ if used |
| `AZURE_TENANT_ID` | |
| `AZURE_SUBSCRIPTION_ID` | |
| `AZURE_RESOURCE_GROUP` | |
| `AZURE_CONTAINER_APP_NAME` | |

If `AZURE_CONTAINER_APP_NAME` is empty, image publish still runs and Azure deploy is skipped.

Rollback: `Deploy backend` workflow_dispatch accepts only `ghcr.io/<owner>/assessflow-backend:<40-char-git-sha>` or `@sha256:<digest>`. Do not rebuild old source with new dependencies. Application rollback is not a Flyway data rollback.

## Azure Container Apps

- External HTTPS ingress, target port **8080**, HTTP transport, WebSockets enabled, `allowInsecure=false`
- Startup/liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- New revisions receive traffic only when ready
- Registry: public GHCR. Anonymous pull; no registry username/password on the Container App
- Restrict ingress to Cloudflare when `APP_HTTP_PROXY_MODE=cloudflare`. Do not hardcode Cloudflare IP lists in Java
- Hikari `DB_POOL_MAX` × replica count must stay within the Neon pooled connection budget

## Cloudflare Pages

- Project root `frontend/`
- Build: `npm ci && npm run build`
- Output `dist`
- Env: `VITE_API_BASE_URL=https://api.<domain>/api/v1`, `VITE_PUBLIC_APP_URL=https://app.<domain>`, `VITE_WS_URL=wss://api.<domain>/ws`
- `_redirects` sends unknown paths to `index.html` so `/login`, `/join/:code`, `/app/...` refresh
- Do not cache `/api/v1/**` or `/ws`
- Enable WebSocket for `api.<domain>`
- WAF may sit in front of the API; authorization stays in Spring

## OpenAPI

`/v3/api-docs` and Swagger UI are on for `local`/`test`. Production keeps them off (`APP_API_DOCS_ENABLED=false`). STOMP is in [websocket.md](websocket.md).

## Flyway

`ddl-auto=validate`. CI applies migrations on empty PostgreSQL. Expand-then-contract: do not drop a column still used by the previous revision in the same deploy.
