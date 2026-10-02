CREATE TABLE live_session_questions (
    id uuid PRIMARY KEY,
    live_session_id uuid NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    source_question_id uuid NOT NULL REFERENCES questions(id),
    question_text varchar(4000) NOT NULL,
    question_type varchar(30) NOT NULL,
    display_order integer NOT NULL,
    points integer NOT NULL,
    CONSTRAINT live_session_questions_points_positive CHECK (points > 0),
    CONSTRAINT live_session_questions_type_check
        CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT live_session_questions_unique UNIQUE (live_session_id, source_question_id)
);

CREATE TABLE live_session_question_options (
    id uuid PRIMARY KEY,
    live_session_question_id uuid NOT NULL REFERENCES live_session_questions(id) ON DELETE CASCADE,
    source_answer_option_id uuid,
    text varchar(2000) NOT NULL,
    correct boolean NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT live_session_question_options_text_not_blank CHECK (length(btrim(text)) > 0)
);

CREATE INDEX live_session_questions_session_order_idx
    ON live_session_questions (live_session_id, display_order);
CREATE INDEX live_session_question_options_question_idx
    ON live_session_question_options (live_session_question_id, display_order);
