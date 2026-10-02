package com.davigama.assessflow.livesession.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_participants")
public class LiveParticipant {
    @Id
    private UUID id;
    @Column(name = "live_session_id", nullable = false)
    private UUID liveSessionId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveParticipantStatus status;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected LiveParticipant() {}

    public LiveParticipant(UUID liveSessionId, String displayName, String tokenHash, Instant now, Instant tokenExpiresAt) {
        this.id = UUID.randomUUID();
        this.liveSessionId = liveSessionId;
        this.displayName = displayName.trim();
        this.status = LiveParticipantStatus.CONNECTED;
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = tokenExpiresAt;
        this.joinedAt = now;
        this.lastSeenAt = now;
    }

    public void connected(Instant now) {
        if (status != LiveParticipantStatus.LEFT) {
            this.status = LiveParticipantStatus.CONNECTED;
        }
        this.lastSeenAt = now;
    }

    public boolean tokenExpired(Instant now) {
        return !now.isBefore(tokenExpiresAt);
    }

    public void expireAt(Instant instant) {
        this.tokenExpiresAt = instant;
    }

    public void disconnected(Instant now) {
        if (status != LiveParticipantStatus.LEFT) {
            this.status = LiveParticipantStatus.DISCONNECTED;
            this.lastSeenAt = now;
        }
    }

    public void leave(Instant now) {
        this.status = LiveParticipantStatus.LEFT;
        this.lastSeenAt = now;
    }

    public UUID getId() { return id; }
    public UUID getLiveSessionId() { return liveSessionId; }
    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public LiveParticipantStatus getStatus() { return status; }
    public String getTokenHash() { return tokenHash; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
