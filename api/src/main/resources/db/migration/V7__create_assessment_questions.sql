CREATE TABLE assessment_questions (
    id uuid PRIMARY KEY,
    assessment_id uuid NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_id uuid NOT NULL REFERENCES questions(id),
    points integer NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT assessment_questions_unique UNIQUE (assessment_id, question_id),
    CONSTRAINT assessment_questions_points_positive CHECK (points > 0)
);

CREATE INDEX assessment_questions_assessment_order_idx
    ON assessment_questions (assessment_id, display_order, id);
