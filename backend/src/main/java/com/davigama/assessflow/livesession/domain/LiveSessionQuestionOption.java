package com.davigama.assessflow.livesession.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "live_session_question_options")
public class LiveSessionQuestionOption {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "live_session_question_id", nullable = false)
    private LiveSessionQuestion question;
    @Column(name = "source_answer_option_id")
    private UUID sourceAnswerOptionId;
    @Column(nullable = false, length = 2000)
    private String text;
    @Column(nullable = false)
    private boolean correct;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected LiveSessionQuestionOption() {}

    public LiveSessionQuestionOption(LiveSessionQuestion question, UUID sourceAnswerOptionId, String text,
                                     boolean correct, int displayOrder) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.sourceAnswerOptionId = sourceAnswerOptionId;
        this.text = text;
        this.correct = correct;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public LiveSessionQuestion getQuestion() { return question; }
    public UUID getSourceAnswerOptionId() { return sourceAnswerOptionId; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
    public int getDisplayOrder() { return displayOrder; }
}
