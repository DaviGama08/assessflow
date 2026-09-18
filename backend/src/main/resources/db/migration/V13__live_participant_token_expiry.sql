ALTER TABLE live_participants
    ADD COLUMN token_expires_at timestamptz NOT NULL DEFAULT (now() + interval '12 hours');

ALTER TABLE live_participants
    ALTER COLUMN token_expires_at DROP DEFAULT;
