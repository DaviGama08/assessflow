package com.davigama.assessflow.questionbank.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestionCategory category;

    @Column(nullable = false, length = 4000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    @Column(length = 4000)
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private List<AnswerOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Question() {}

    public Question(UUID organizationId, QuestionCategory category, String text, QuestionType type,
                    QuestionDifficulty difficulty, String explanation, QuestionStatus status, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.category = category;
        this.text = text.trim();
        this.type = type;
        this.difficulty = difficulty;
        this.explanation = explanation;
        this.status = status == null ? QuestionStatus.DRAFT : status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(QuestionCategory category, String text, QuestionType type, QuestionDifficulty difficulty,
                       String explanation, QuestionStatus status, Instant now) {
        this.category = category;
        this.text = text.trim();
        this.type = type;
        this.difficulty = difficulty;
        this.explanation = explanation;
        if (status != null) this.status = status;
        this.updatedAt = now;
    }

    public void replaceOptions(List<AnswerOption> next) {
        QuestionRules.requireValid(type, next);
        options.clear();
        options.addAll(next);
    }

    public void archive(Instant now) {
        this.status = QuestionStatus.ARCHIVED;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public QuestionCategory getCategory() { return category; }
    public String getText() { return text; }
    public QuestionType getType() { return type; }
    public QuestionDifficulty getDifficulty() { return difficulty; }
    public QuestionStatus getStatus() { return status; }
    public String getExplanation() { return explanation; }
    public List<AnswerOption> getOptions() { return options; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
