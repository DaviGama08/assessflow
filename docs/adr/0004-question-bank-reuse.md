# ADR 0004: Reusable question bank

**Status:** Accepted

## Context

Assessments need questions, but the same item should be reusable across assessments in an organization. Putting `assessment_id` on `Question` would copy or lock a question to a single assessment.

## Decision

`Question` is an organization-owned bank entity. It has text, type, difficulty, status, explanation, category and answer options. It has no assessment foreign key.

`AssessmentQuestion` is the association between an assessment and a bank question. It stores `points` and `display_order` because scoring and sequence belong to that assessment, not to the shared question. The pair `(assessment_id, question_id)` is unique.

Questions remain mutable in the bank. This phase does not snapshot question text or options when they are added to an assessment. Editing a bank question therefore changes it for every assessment that references it. Historical versioning, event sourcing and immutable snapshots are deferred.

## Consequences

Authors can assemble assessments from a shared bank and assign points and order per assessment. Cross-tenant linking is impossible because both sides are scoped to `organization_id`. Later live-session work can add snapshots if frozen historical content becomes a requirement.
