package com.davigama.assessflow.questionbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "question_categories")
public class QuestionCategory {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuestionCategory() {}

    public QuestionCategory(UUID organizationId, String name, String slug, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.name = name.trim();
        this.slug = slug;
        this.createdAt = now;
    }

    public void rename(String name, String slug) {
        this.name = name.trim();
        this.slug = slug;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
}
