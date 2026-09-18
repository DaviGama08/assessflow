package com.davigama.distributedquiz.assessment.api.dto;

import com.davigama.distributedquiz.assessment.domain.Assessment;
import com.davigama.distributedquiz.assessment.domain.AssessmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentResponse(UUID id, String title, String description, AssessmentStatus status,
                                 Instant createdAt, Instant updatedAt) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(assessment.getId(), assessment.getTitle(),
                assessment.getDescription(), assessment.getStatus(),
                assessment.getCreatedAt(), assessment.getUpdatedAt());
    }
}
