CREATE TABLE live_participants (
    id uuid PRIMARY KEY,
    live_session_id uuid NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    user_id uuid REFERENCES users(id),
    display_name varchar(200) NOT NULL,
    status varchar(20) NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE,
    joined_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    CONSTRAINT live_participants_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT live_participants_status_check CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'LEFT'))
);

CREATE INDEX live_participants_session_idx ON live_participants (live_session_id, status);
