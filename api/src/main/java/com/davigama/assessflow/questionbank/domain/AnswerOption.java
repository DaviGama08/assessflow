package com.davigama.assessflow.questionbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "answer_options")
public class AnswerOption {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected AnswerOption() {}

    public AnswerOption(Question question, String text, boolean correct, int displayOrder) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.text = text.trim();
        this.correct = correct;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
    public int getDisplayOrder() { return displayOrder; }
}
