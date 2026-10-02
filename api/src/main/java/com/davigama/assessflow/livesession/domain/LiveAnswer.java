package com.davigama.assessflow.livesession.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "live_answers")
public class LiveAnswer {
    @Id
    private UUID id;
    @Column(name = "live_session_id", nullable = false)
    private UUID liveSessionId;
    @Column(name = "live_session_question_id", nullable = false)
    private UUID liveSessionQuestionId;
    @Column(name = "participant_id", nullable = false)
    private UUID participantId;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "live_answer_options", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id")
    private Set<UUID> optionIds = new HashSet<>();

    protected LiveAnswer() {}

    public LiveAnswer(UUID liveSessionId, UUID liveSessionQuestionId, UUID participantId, Set<UUID> optionIds,
                      Instant now) {
        this.id = UUID.randomUUID();
        this.liveSessionId = liveSessionId;
        this.liveSessionQuestionId = liveSessionQuestionId;
        this.participantId = participantId;
        this.optionIds = new HashSet<>(optionIds);
        this.submittedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getLiveSessionId() { return liveSessionId; }
    public UUID getLiveSessionQuestionId() { return liveSessionQuestionId; }
    public UUID getParticipantId() { return participantId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Set<UUID> getOptionIds() { return optionIds; }
}
