# AssessFlow architecture

AssessFlow is a modular Spring Boot monolith with a React SPA. Feature packages follow API, application, domain and infrastructure layers. JPA entities never leave the API; controllers return DTO records. Flyway owns the schema. Hibernate runs with `ddl-auto=validate` and `open-in-view=false`. Collections are LAZY; queries that need associations use an explicit fetch graph.

## Identity and authentication

Users register with a unique email and a BCrypt password hash. Opaque access and refresh tokens are stored as SHA-256 hashes. The SPA keeps the access token in memory and the refresh token in an HttpOnly cookie. See [ADR 0002](adr/0002-authentication-strategy.md).

## Organizations and roles

An organization is the tenant. Membership binds a user to an organization with role OWNER, ADMIN, INSTRUCTOR or PARTICIPANT. The last active OWNER cannot be removed or demoted. Member management is limited to OWNER and ADMIN.

## Tenant boundaries

Tenant-owned rows include `organization_id`. Lookups use organization scope, not `findById` alone. Cross-tenant access returns 404 for the nested resource and 403 when the caller is not a member of the organization in the URL. See [ADR 0003](adr/0003-multi-tenancy-strategy.md).

```text
User ──< OrganizationMember >── Organization
                                  │
                                  ├── Assessment ──< AssessmentQuestion
                                  ├── Question <────────────┘
                                  ├── QuestionCategory
                                  └── OrganizationBranding
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

Authenticated users pick an organization, then work inside `/app/organizations/:organizationId` with Dashboard, Assessments, Question Bank, Members and Settings. Navigation is role-aware. The API remains the authority.

Live sessions, WebSocket, join codes, QR codes, Redis and brokers are out of scope until Phase 3.
