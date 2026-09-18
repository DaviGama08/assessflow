package com.davigama.assessflow.assessment.domain;

import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "assessments")
public class Assessment {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssessmentStatus status;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions;

    @Column(name = "shuffle_answers", nullable = false)
    private boolean shuffleAnswers;

    @Column(name = "show_results_after_completion", nullable = false)
    private boolean showResultsAfterCompletion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Assessment() {}

    public Assessment(UUID organizationId, String title, String description, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.title = title.trim();
        this.description = description;
        this.status = AssessmentStatus.DRAFT;
        this.maxAttempts = 1;
        this.shuffleQuestions = false;
        this.shuffleAnswers = false;
        this.showResultsAfterCompletion = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, Instant now) {
        this.title = title.trim();
        this.description = description;
        this.updatedAt = now;
    }

    public void configure(Integer timeLimitMinutes, Integer maxAttempts, Integer passingScore,
                          Boolean shuffleQuestions, Boolean shuffleAnswers,
                          Boolean showResultsAfterCompletion, Instant now) {
        if (timeLimitMinutes != null && timeLimitMinutes <= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_TIME_LIMIT",
                    "Time limit must be greater than 0 minutes.");
        }
        int attempts = maxAttempts != null ? maxAttempts : this.maxAttempts;
        if (attempts < 1) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_MAX_ATTEMPTS",
                    "Maximum attempts must be at least 1.");
        }
        if (passingScore != null && (passingScore < 0 || passingScore > 100)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PASSING_SCORE",
                    "Passing score must be between 0 and 100.");
        }
        this.timeLimitMinutes = timeLimitMinutes;
        this.maxAttempts = attempts;
        this.passingScore = passingScore;
        if (shuffleQuestions != null) this.shuffleQuestions = shuffleQuestions;
        if (shuffleAnswers != null) this.shuffleAnswers = shuffleAnswers;
        if (showResultsAfterCompletion != null) this.showResultsAfterCompletion = showResultsAfterCompletion;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AssessmentStatus getStatus() { return status; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public int getMaxAttempts() { return maxAttempts; }
    public Integer getPassingScore() { return passingScore; }
    public boolean isShuffleQuestions() { return shuffleQuestions; }
    public boolean isShuffleAnswers() { return shuffleAnswers; }
    public boolean isShowResultsAfterCompletion() { return showResultsAfterCompletion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
