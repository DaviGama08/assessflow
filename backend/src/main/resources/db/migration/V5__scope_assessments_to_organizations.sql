-- Assessments created before tenant scoping have no organization owner.
-- They are development data and cannot be attributed safely, so they are removed.
DELETE FROM assessments;

ALTER TABLE assessments
    ADD COLUMN organization_id uuid NOT NULL REFERENCES organizations(id),
    ADD COLUMN time_limit_minutes integer,
    ADD COLUMN max_attempts integer NOT NULL DEFAULT 1,
    ADD COLUMN passing_score integer,
    ADD COLUMN shuffle_questions boolean NOT NULL DEFAULT false,
    ADD COLUMN shuffle_answers boolean NOT NULL DEFAULT false,
    ADD COLUMN show_results_after_completion boolean NOT NULL DEFAULT true;

ALTER TABLE assessments
    ADD CONSTRAINT assessments_time_limit_positive CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    ADD CONSTRAINT assessments_max_attempts_min CHECK (max_attempts >= 1),
    ADD CONSTRAINT assessments_passing_score_range
        CHECK (passing_score IS NULL OR (passing_score >= 0 AND passing_score <= 100));

CREATE INDEX assessments_organization_created_idx
    ON assessments (organization_id, created_at DESC, id DESC);
