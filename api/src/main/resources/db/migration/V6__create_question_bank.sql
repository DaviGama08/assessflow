CREATE TABLE question_categories (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id),
    name varchar(200) NOT NULL,
    slug varchar(100) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT question_categories_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT question_categories_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT question_categories_org_slug_unique UNIQUE (organization_id, slug)
);

CREATE TABLE questions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id),
    category_id uuid NOT NULL REFERENCES question_categories(id),
    text varchar(4000) NOT NULL,
    type varchar(30) NOT NULL,
    difficulty varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    explanation varchar(4000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT questions_text_not_blank CHECK (length(btrim(text)) > 0),
    CONSTRAINT questions_type_check CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT questions_difficulty_check CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT questions_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE answer_options (
    id uuid PRIMARY KEY,
    question_id uuid NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text varchar(2000) NOT NULL,
    correct boolean NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT answer_options_text_not_blank CHECK (length(btrim(text)) > 0)
);

CREATE INDEX questions_organization_id_idx ON questions (organization_id);
CREATE INDEX questions_organization_status_idx ON questions (organization_id, status);
CREATE INDEX questions_organization_category_idx ON questions (organization_id, category_id);
CREATE INDEX answer_options_question_id_idx ON answer_options (question_id);
