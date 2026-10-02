package com.davigama.assessflow.organization.domain;

import com.davigama.assessflow.identity.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_members")
public class OrganizationMember {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MemberRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MemberStatus status;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    protected OrganizationMember() {}
    public OrganizationMember(Organization organization, User user, MemberRole role, Instant now) {
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = now;
    }
    public void changeRole(MemberRole role) { this.role = role; }
    public void reactivate(MemberRole role, Instant now) {
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = now;
    }
    public void disable() { this.status = MemberStatus.DISABLED; }
    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getUser() { return user; }
    public MemberRole getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
}
