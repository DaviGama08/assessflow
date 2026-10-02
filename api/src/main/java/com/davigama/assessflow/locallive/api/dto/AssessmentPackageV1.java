package com.davigama.assessflow.locallive.api.dto;

import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssessmentPackageV1(
        @NotNull @Min(1) Integer schemaVersion,
        @NotNull Instant exportedAt,
        @NotNull UUID sourceOrganizationId,
        @NotBlank @Size(max = 200) String organizationName,
        @NotNull @Valid PackageAssessment assessment,
        @NotEmpty @Size(max = 200) List<@Valid PackageQuestion> questions,
        @NotBlank @Size(min = 64, max = 64) String checksumSha256
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * SHA-256 package checksum detects corruption.
     * It does not prove package authenticity.
     */
    public PackageContent content() {
        return new PackageContent(schemaVersion, exportedAt, sourceOrganizationId, organizationName, assessment, questions);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackageContent(
            Integer schemaVersion,
            Instant exportedAt,
            UUID sourceOrganizationId,
            String organizationName,
            PackageAssessment assessment,
            List<PackageQuestion> questions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackageAssessment(
            @NotNull UUID sourceId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Min(1) Integer timeLimitMinutes,
            @NotNull @Min(1) Integer maxAttempts,
            @Min(0) @Max(100) Integer passingScore,
            boolean shuffleQuestions,
            boolean shuffleAnswers,
            boolean showResultsAfterCompletion
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackageQuestion(
            @NotNull UUID sourceQuestionId,
            @NotBlank @Size(max = 4000) String text,
            @NotNull QuestionType type,
            @NotNull QuestionDifficulty difficulty,
            @Size(max = 4000) String explanation,
            @NotBlank @Size(max = 200) String category,
            @Min(1) @Max(10_000) int points,
            @Min(0) int displayOrder,
            @NotEmpty @Size(min = 2, max = 20) List<@Valid PackageOption> options
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackageOption(
            @NotBlank @Size(max = 2000) String text,
            boolean correct,
            @Min(0) int displayOrder
    ) {}
}
