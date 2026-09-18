package com.davigama.assessflow.assessment.api.dto;

import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentResponse(
        UUID id,
        UUID organizationId,
        String title,
        String description,
        AssessmentStatus status,
        Integer timeLimitMinutes,
        int maxAttempts,
        Integer passingScore,
        boolean shuffleQuestions,
        boolean shuffleAnswers,
        boolean showResultsAfterCompletion,
        Instant createdAt,
        Instant updatedAt) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getOrganizationId(),
                assessment.getTitle(),
                assessment.getDescription(),
                assessment.getStatus(),
                assessment.getTimeLimitMinutes(),
                assessment.getMaxAttempts(),
                assessment.getPassingScore(),
                assessment.isShuffleQuestions(),
                assessment.isShuffleAnswers(),
                assessment.isShowResultsAfterCompletion(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt());
    }
}
