package com.davigama.assessflow.assessment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAssessmentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description) {}
