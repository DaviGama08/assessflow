package com.davigama.assessflow.assessment.api.dto;

import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class AssessmentQuestionDtos {
    private AssessmentQuestionDtos() {}

    public record AddAssessmentQuestionRequest(@NotNull @Min(1) Integer points, Integer displayOrder) {}

    public record UpdateAssessmentQuestionRequest(@NotNull @Min(1) Integer points) {}

    public record ReorderAssessmentQuestionsRequest(@NotEmpty List<UUID> questionIds) {}

    public record AssessmentQuestionResponse(
            UUID id,
            UUID assessmentId,
            UUID questionId,
            String questionText,
            QuestionType type,
            QuestionDifficulty difficulty,
            QuestionStatus status,
            int points,
            int displayOrder) {
        public static AssessmentQuestionResponse from(AssessmentQuestion link, Question question) {
            return new AssessmentQuestionResponse(
                    link.getId(),
                    link.getAssessmentId(),
                    link.getQuestionId(),
                    question.getText(),
                    question.getType(),
                    question.getDifficulty(),
                    question.getStatus(),
                    link.getPoints(),
                    link.getDisplayOrder());
        }
    }
}
