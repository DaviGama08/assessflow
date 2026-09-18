package com.davigama.assessflow.organization.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 100) private String slug;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrganizationStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Organization() {}
    public Organization(String name, String slug, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name.trim();
        this.slug = slug;
        this.status = OrganizationStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }
    public void rename(String name, Instant now) {
        this.name = name.trim();
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public OrganizationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
