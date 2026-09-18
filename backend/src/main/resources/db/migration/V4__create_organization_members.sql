CREATE TABLE organization_members (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(id),
    user_id uuid NOT NULL REFERENCES users(id),
    role varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    joined_at timestamptz NOT NULL,
    CONSTRAINT organization_members_unique_user UNIQUE (organization_id, user_id),
    CONSTRAINT organization_members_role_check CHECK (role IN ('OWNER', 'ADMIN', 'INSTRUCTOR', 'PARTICIPANT')),
    CONSTRAINT organization_members_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);
CREATE INDEX organization_members_user_status_idx ON organization_members(user_id, status);
