package com.davigama.assessflow.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessments")
public class Assessment {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssessmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Assessment() {}

    public Assessment(String title, String description, Instant now) {
        this.id = UUID.randomUUID();
        this.title = title.trim();
        this.description = description;
        this.status = AssessmentStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, Instant now) {
        this.title = title.trim();
        this.description = description;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AssessmentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
