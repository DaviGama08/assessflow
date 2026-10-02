package com.davigama.assessflow.livesession.domain;

import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.shared.exception.DomainException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public final class AnswerSelections {
    private AnswerSelections() {}

    public static Set<UUID> validate(QuestionType type, List<UUID> optionIds, Set<UUID> allowed) {
        if (optionIds == null || optionIds.isEmpty()) {
            throw invalid("Select at least one option.");
        }
        Set<UUID> unique = new HashSet<>();
        for (UUID optionId : optionIds) {
            if (optionId == null || !unique.add(optionId)) {
                throw invalid("Duplicate option IDs are not allowed.");
            }
        }
        if (!allowed.containsAll(unique)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_ANSWER_OPTION",
                    "One or more options do not belong to the current question.");
        }
        if ((type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE) && unique.size() != 1) {
            throw invalid("This question accepts exactly one option.");
        }
        return unique;
    }

    private static DomainException invalid(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_ANSWER_SELECTION", message);
    }
}
