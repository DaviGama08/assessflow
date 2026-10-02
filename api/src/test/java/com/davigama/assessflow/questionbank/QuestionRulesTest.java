package com.davigama.assessflow.questionbank;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionRules;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.shared.exception.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionRulesTest {
    private final Question question = new Question(UUID.randomUUID(),
            new QuestionCategory(UUID.randomUUID(), "Core", "core", Instant.parse("2026-09-18T12:00:00Z")),
            "Text", QuestionType.SINGLE_CHOICE, QuestionDifficulty.EASY, null, QuestionStatus.DRAFT,
            Instant.parse("2026-09-18T12:00:00Z"));

    @Test
    void rejectsInvalidOptionSets() {
        assertThatThrownBy(() -> QuestionRules.requireValid(QuestionType.SINGLE_CHOICE, List.of(
                option("A", true), option("B", true)))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> QuestionRules.requireValid(QuestionType.MULTIPLE_CHOICE, List.of(
                option("A", false), option("B", false)))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> QuestionRules.requireValid(QuestionType.TRUE_FALSE, List.of(
                option("True", true)))).isInstanceOf(DomainException.class);
        QuestionRules.requireValid(QuestionType.SINGLE_CHOICE, List.of(option("A", true), option("B", false)));
        QuestionRules.requireValid(QuestionType.MULTIPLE_CHOICE, List.of(option("A", true), option("B", true)));
        QuestionRules.requireValid(QuestionType.TRUE_FALSE, List.of(option("True", true), option("False", false)));
    }

    private AnswerOption option(String text, boolean correct) {
        return new AnswerOption(question, text, correct, 0);
    }
}
