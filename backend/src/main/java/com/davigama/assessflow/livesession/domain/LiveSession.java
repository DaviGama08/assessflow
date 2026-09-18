package com.davigama.assessflow.livesession.domain;

import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "live_sessions")
public class LiveSession {
    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;
    @Column(name = "join_code", nullable = false, length = 6)
    private String joinCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveSessionStatus status;
    @Column(name = "current_question_index")
    private Integer currentQuestionIndex;
    @Column(name = "current_question_started_at")
    private Instant currentQuestionStartedAt;
    @Column(name = "question_open", nullable = false)
    private boolean questionOpen;
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected LiveSession() {}

    public LiveSession(UUID organizationId, UUID assessmentId, String joinCode, UUID createdByUserId, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.assessmentId = assessmentId;
        this.joinCode = joinCode;
        this.status = LiveSessionStatus.WAITING;
        this.questionOpen = false;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void start(Instant now) {
        requireStatus(LiveSessionStatus.WAITING, "LIVE_SESSION_ALREADY_STARTED",
                "Only a waiting session can be started.");
        this.status = LiveSessionStatus.ACTIVE;
        this.startedAt = now;
        openQuestion(0, now);
    }

    public void endQuestion(Instant now) {
        requireActive();
        if (!questionOpen) {
            throw conflict("QUESTION_NOT_ACTIVE", "There is no open question to end.");
        }
        this.questionOpen = false;
        this.updatedAt = now;
    }

    public void nextQuestion(int nextIndex, Instant now) {
        requireActive();
        if (questionOpen) {
            throw conflict("QUESTION_STILL_OPEN", "End the current question before moving on.");
        }
        if (currentQuestionIndex == null || nextIndex != currentQuestionIndex + 1) {
            throw conflict("INVALID_QUESTION_PROGRESSION", "Questions must advance in order.");
        }
        openQuestion(nextIndex, now);
    }

    public void finish(Instant now) {
        requireActive();
        this.status = LiveSessionStatus.FINISHED;
        this.questionOpen = false;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status == LiveSessionStatus.FINISHED || status == LiveSessionStatus.CANCELLED) {
            throw conflict("LIVE_SESSION_FINISHED", "This session can no longer be cancelled.");
        }
        this.status = LiveSessionStatus.CANCELLED;
        this.questionOpen = false;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public boolean joinable() {
        return status == LiveSessionStatus.WAITING || status == LiveSessionStatus.ACTIVE;
    }

    public void requireJoinable() {
        if (!joinable()) {
            throw new DomainException(HttpStatus.CONFLICT, "LIVE_SESSION_NOT_JOINABLE",
                    "This session is no longer accepting participants.");
        }
    }

    public void requireActive() {
        requireStatus(LiveSessionStatus.ACTIVE, "LIVE_SESSION_FINISHED", "This session is not active.");
    }

    public void requireQuestionOpen() {
        requireActive();
        if (!questionOpen) throw conflict("QUESTION_NOT_ACTIVE", "The current question is not open for answers.");
    }

    private void openQuestion(int index, Instant now) {
        this.currentQuestionIndex = index;
        this.currentQuestionStartedAt = now;
        this.questionOpen = true;
        this.updatedAt = now;
    }

    private void requireStatus(LiveSessionStatus expected, String code, String message) {
        if (status != expected) throw conflict(code, message);
    }

    private DomainException conflict(String code, String message) {
        return new DomainException(HttpStatus.CONFLICT, code, message);
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getAssessmentId() { return assessmentId; }
    public String getJoinCode() { return joinCode; }
    public LiveSessionStatus getStatus() { return status; }
    public Integer getCurrentQuestionIndex() { return currentQuestionIndex; }
    public Instant getCurrentQuestionStartedAt() { return currentQuestionStartedAt; }
    public boolean isQuestionOpen() { return questionOpen; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
