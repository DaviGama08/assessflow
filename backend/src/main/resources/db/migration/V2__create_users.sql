CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(320) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    display_name varchar(200) NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT users_email_normalized CHECK (email = lower(btrim(email)) AND length(email) > 0),
    CONSTRAINT users_display_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE auth_tokens (
    token_hash varchar(64) PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind varchar(10) NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT auth_tokens_kind_check CHECK (kind IN ('ACCESS', 'REFRESH'))
);
CREATE INDEX auth_tokens_user_id_idx ON auth_tokens(user_id);
