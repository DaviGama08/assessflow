CREATE TABLE assessments (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL,
    description varchar(2000),
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT assessments_title_not_blank CHECK (length(btrim(title)) > 0),
    CONSTRAINT assessments_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);
