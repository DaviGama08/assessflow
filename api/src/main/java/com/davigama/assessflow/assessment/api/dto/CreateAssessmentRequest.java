package com.davigama.assessflow.assessment.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAssessmentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Positive Integer timeLimitMinutes,
        @Min(1) Integer maxAttempts,
        @Min(0) @Max(100) Integer passingScore,
        Boolean shuffleQuestions,
        Boolean shuffleAnswers,
        Boolean showResultsAfterCompletion) {}
