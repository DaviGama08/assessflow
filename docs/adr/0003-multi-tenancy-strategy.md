# ADR 0003: Multi-tenancy strategy

**Status:** Accepted

## Context

AssessFlow organizations are isolated workspaces. Assessments, questions and branding belong to exactly one organization. Users may belong to several organizations with different roles.

## Decision

Use a shared PostgreSQL database and a shared schema. Tenant isolation is a discriminator column, `organization_id`, on every tenant-owned table. Foreign keys point at `organizations(id)`. Queries for tenant resources never load by primary key alone. They use an organization-scoped lookup such as `findByIdAndOrganizationId`. A resource that exists in another organization is returned as 404 so callers cannot probe across tenants.

Authorization is separate from scoping. Spring Security authenticates the user. Organization membership and role then decide whether the caller may act. OWNER, ADMIN and INSTRUCTOR may manage assessments and the question bank. OWNER and ADMIN may manage members and branding. PARTICIPANT has no management access in this phase. A user who belongs to two organizations can access each workspace, but cannot address Organization A's resources through Organization B's URLs.

Existing assessments created before this change had no owner. They are development data and are deleted in `V5__scope_assessments_to_organizations.sql` before `organization_id` becomes required.

## Consequences

Tenant isolation is enforced in application queries and in the database, not by separate schemas or databases. This keeps local setup and Flyway migrations simple. Every new tenant-owned entity must include `organization_id`, a foreign key, an index, and scoped repository methods. Cross-tenant association of questions to assessments is rejected because both sides must share the same organization.
