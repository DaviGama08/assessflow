CREATE TABLE live_answers (
    id uuid PRIMARY KEY,
    live_session_id uuid NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    live_session_question_id uuid NOT NULL REFERENCES live_session_questions(id) ON DELETE CASCADE,
    participant_id uuid NOT NULL REFERENCES live_participants(id) ON DELETE CASCADE,
    submitted_at timestamptz NOT NULL,
    CONSTRAINT live_answers_unique UNIQUE (live_session_id, live_session_question_id, participant_id)
);

CREATE TABLE live_answer_options (
    answer_id uuid NOT NULL REFERENCES live_answers(id) ON DELETE CASCADE,
    option_id uuid NOT NULL REFERENCES live_session_question_options(id),
    PRIMARY KEY (answer_id, option_id)
);

CREATE INDEX live_answers_session_question_idx
    ON live_answers (live_session_id, live_session_question_id);
