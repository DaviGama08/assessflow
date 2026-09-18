CREATE TABLE organizations (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    slug varchar(100) NOT NULL UNIQUE,
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT organizations_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT organizations_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT organizations_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);
