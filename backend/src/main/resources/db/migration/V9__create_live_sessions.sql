CREATE TABLE live_sessions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id),
    assessment_id uuid NOT NULL REFERENCES assessments(id),
    join_code varchar(6) NOT NULL,
    status varchar(20) NOT NULL,
    current_question_index integer,
    current_question_started_at timestamptz,
    question_open boolean NOT NULL DEFAULT false,
    created_by_user_id uuid NOT NULL REFERENCES users(id),
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT live_sessions_status_check CHECK (status IN ('WAITING', 'ACTIVE', 'FINISHED', 'CANCELLED')),
    CONSTRAINT live_sessions_join_code_format CHECK (join_code ~ '^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$'),
    CONSTRAINT live_sessions_join_code_unique UNIQUE (join_code)
);

CREATE INDEX live_sessions_organization_status_idx ON live_sessions (organization_id, status);
CREATE INDEX live_sessions_assessment_id_idx ON live_sessions (assessment_id);
