package com.davigama.assessflow.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    public enum Kind { ACCESS, REFRESH }
    @Id @Column(name = "token_hash", length = 64) private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Kind kind;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    protected AuthToken() {}
    public AuthToken(String tokenHash, User user, Kind kind, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.kind = kind;
        this.expiresAt = expiresAt;
    }
    public String getTokenHash() { return tokenHash; }
    public User getUser() { return user; }
    public Kind getKind() { return kind; }
    public Instant getExpiresAt() { return expiresAt; }
}
