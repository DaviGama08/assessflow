package com.davigama.assessflow.questionbank.api.dto;

import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public final class QuestionDtos {
    private QuestionDtos() {}

    public record CreateCategoryRequest(@NotBlank @Size(max = 200) String name, @NotBlank String slug) {}

    public record UpdateCategoryRequest(@NotBlank @Size(max = 200) String name, @NotBlank String slug) {}

    public record CategoryResponse(UUID id, UUID organizationId, String name, String slug, Instant createdAt) {
        public static CategoryResponse from(QuestionCategory category) {
            return new CategoryResponse(category.getId(), category.getOrganizationId(), category.getName(),
                    category.getSlug(), category.getCreatedAt());
        }
    }

    public record OptionInput(@NotBlank @Size(max = 2000) String text, @NotNull Boolean correct, Integer displayOrder) {}

    public record CreateQuestionRequest(
            @NotBlank @Size(max = 4000) String text,
            @NotNull QuestionType type,
            @NotNull QuestionDifficulty difficulty,
            @NotNull UUID categoryId,
            @Size(max = 4000) String explanation,
            QuestionStatus status,
            @NotEmpty List<@Valid OptionInput> options) {}

    public record UpdateQuestionRequest(
            @NotBlank @Size(max = 4000) String text,
            @NotNull QuestionType type,
            @NotNull QuestionDifficulty difficulty,
            @NotNull UUID categoryId,
            @Size(max = 4000) String explanation,
            QuestionStatus status,
            @NotEmpty List<@Valid OptionInput> options) {}

    public record OptionResponse(UUID id, String text, boolean correct, int displayOrder) {
        public static OptionResponse from(AnswerOption option) {
            return new OptionResponse(option.getId(), option.getText(), option.isCorrect(), option.getDisplayOrder());
        }
    }

    public record QuestionResponse(
            UUID id,
            UUID organizationId,
            String text,
            QuestionType type,
            QuestionDifficulty difficulty,
            QuestionStatus status,
            String explanation,
            CategoryResponse category,
            List<OptionResponse> options,
            Instant createdAt,
            Instant updatedAt) {
        public static QuestionResponse from(Question question, boolean includeOptions) {
            return new QuestionResponse(
                    question.getId(),
                    question.getOrganizationId(),
                    question.getText(),
                    question.getType(),
                    question.getDifficulty(),
                    question.getStatus(),
                    question.getExplanation(),
                    CategoryResponse.from(question.getCategory()),
                    includeOptions
                            ? question.getOptions().stream().map(OptionResponse::from).toList()
                            : List.of(),
                    question.getCreatedAt(),
                    question.getUpdatedAt());
        }
    }

    public record QuestionPageResponse(
            List<QuestionResponse> content,
            int number,
            int totalPages,
            long totalElements,
            boolean first,
            boolean last) {
        public static QuestionPageResponse from(Page<QuestionResponse> page) {
            return new QuestionPageResponse(page.getContent(), page.getNumber(), page.getTotalPages(),
                    page.getTotalElements(), page.isFirst(), page.isLast());
        }
    }
}
