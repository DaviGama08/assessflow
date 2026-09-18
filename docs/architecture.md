# AssessFlow architecture

AssessFlow is a modular Spring Boot monolith with a React SPA. Feature packages follow API, application, domain and infrastructure layers. JPA entities never leave the API; controllers return DTO records. Flyway owns the schema. Hibernate runs with `ddl-auto=validate` and `open-in-view=false`. Collections are LAZY; queries that need associations use an explicit fetch graph.

## Identity and authentication

Users register with a unique email and a BCrypt password hash. Opaque access and refresh tokens are stored as SHA-256 hashes. The SPA keeps the access token in memory and the refresh token in an HttpOnly cookie. See [ADR 0002](adr/0002-authentication-strategy.md).

## Organizations and roles

An organization is the tenant. Membership binds a user to an organization with role OWNER, ADMIN, INSTRUCTOR or PARTICIPANT. The last active OWNER cannot be removed or demoted. Member management is limited to OWNER and ADMIN. Any active member may view the paginated member list; INSTRUCTOR and PARTICIPANT do not see add, role or remove controls. Direct `/settings` URLs are blocked in the SPA for non-managers, and the API still rejects unauthorized edits.

Assessments start as DRAFT. OWNER, ADMIN and INSTRUCTOR publish through `POST .../publish` only when at least one ACTIVE question is linked. Publish and archive are domain transitions (`DRAFT → PUBLISHED`, `DRAFT|PUBLISHED → ARCHIVED`); status cannot be set by a generic DTO.

## Tenant boundaries

Tenant-owned rows include `organization_id`. Lookups use organization scope, not `findById` alone. Cross-tenant access returns 404 for the nested resource and 403 when the caller is not a member of the organization in the URL. See [ADR 0003](adr/0003-multi-tenancy-strategy.md).

```text
User ──< OrganizationMember >── Organization
                                  │
                                  ├── Assessment ──< AssessmentQuestion
                                  ├── Question <────────────┘
                                  ├── QuestionCategory
                                  ├── OrganizationBranding
                                  └── LiveSession
                                        ├── LiveSessionQuestion ──< LiveSessionQuestionOption
                                        ├── LiveParticipant
                                        └── LiveAnswer
```

## Assessments

Assessments belong to one organization. OWNER, ADMIN and INSTRUCTOR may create, list, update and delete them. Configuration covers time limit, attempts, passing score and shuffle/result flags.

## Question bank

Questions are reusable inside an organization. Types are single choice, multiple choice and true/false. The backend validates option cardinality and correctness. Categories are unique per organization slug. Delete archives a question.

## Assessment builder

`AssessmentQuestion` links a bank question to an assessment with points and display order. Points and order live on the association. Questions are not copied; see [ADR 0004](adr/0004-question-bank-reuse.md).

## Branding

Each organization has optional branding: display name, logo URL and `#RRGGBB` colors. The backend does not download logos or accept custom HTML, CSS or JavaScript. OWNER and ADMIN may change branding. The product name remains AssessFlow; branding labels the customer workspace.

## Frontend

Authenticated users pick an organization, then work inside `/app/organizations/:organizationId` with Dashboard, Assessments, Question Bank, Members and Settings. Navigation is role-aware. The API remains the authority. Public join routes `/join` and `/join/:code` do not require login.

## Live sessions

OWNER, ADMIN and INSTRUCTOR create a live session from a **PUBLISHED** assessment. The API copies questions and options into `LiveSessionQuestion` snapshots so later bank edits do not change an in-flight session. PostgreSQL stores status, the current question, participants and answers. REST handles commands; STOMP at `/ws` notifies `/topic/sessions/{sessionId}`. Guests join with a six-character code and receive an opaque participant token hashed with SHA-256. Tokens expire after `APP_LIVE_PARTICIPANT_TOKEN_TTL` (default 12 hours) so results can still be read at the end of an event. Duplicate option IDs are rejected; single-choice and true/false answers must contain exactly one option. STOMP participants subscribe to `/topic/sessions/{id}`; hosts subscribe to `/topic/host/sessions/{id}`. WebSocket origins follow `APP_WS_ALLOWED_ORIGINS` or `APP_CORS_ALLOWED_ORIGINS` — there is no wildcard. The QR encodes only `{VITE_PUBLIC_APP_URL}/join/{code}`. See [ADR 0005](adr/0005-live-session-realtime.md).

Local Live Mode uses profile `local-live`: same-origin SPA + API + WebSocket, PostgreSQL on localhost, LAN join URLs, assessment packages and an optional Ubuntu hotspot. See [ADR 0006](adr/0006-local-live-mode.md) and [local-live.md](local-live.md). Redis and message brokers remain out of scope.
