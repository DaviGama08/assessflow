package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.livesession.domain.AnswerSelections;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.shared.exception.DomainException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerSelectionsTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final Set<UUID> allowed = Set.of(a, b);

    @Test
    void singleChoiceRequiresExactlyOneOption() {
        assertThat(AnswerSelections.validate(QuestionType.SINGLE_CHOICE, List.of(a), allowed)).containsExactly(a);
        assertThatThrownBy(() -> AnswerSelections.validate(QuestionType.SINGLE_CHOICE, List.of(a, b), allowed))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("INVALID_ANSWER_SELECTION");
        assertThatThrownBy(() -> AnswerSelections.validate(QuestionType.TRUE_FALSE, List.of(a, b), allowed))
                .extracting("code")
                .isEqualTo("INVALID_ANSWER_SELECTION");
    }

    @Test
    void multipleChoiceAllowsOneOrMoreAndRejectsDuplicates() {
        assertThat(AnswerSelections.validate(QuestionType.MULTIPLE_CHOICE, List.of(a, b), allowed)).containsExactlyInAnyOrder(a, b);
        assertThatThrownBy(() -> AnswerSelections.validate(QuestionType.MULTIPLE_CHOICE, List.of(a, a), allowed))
                .extracting("code")
                .isEqualTo("INVALID_ANSWER_SELECTION");
        assertThatThrownBy(() -> AnswerSelections.validate(QuestionType.SINGLE_CHOICE, List.of(UUID.randomUUID()), allowed))
                .extracting("code")
                .isEqualTo("INVALID_ANSWER_OPTION");
    }
}
