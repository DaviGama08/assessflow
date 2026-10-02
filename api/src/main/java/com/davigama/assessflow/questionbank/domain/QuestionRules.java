package com.davigama.assessflow.questionbank.domain;

import com.davigama.assessflow.shared.exception.DomainException;
import java.util.List;
import org.springframework.http.HttpStatus;

public final class QuestionRules {
    private QuestionRules() {}

    public static void requireValid(QuestionType type, List<AnswerOption> options) {
        if (options == null) {
            throw invalid("Question options are required.");
        }
        long correct = options.stream().filter(AnswerOption::isCorrect).count();
        switch (type) {
            case SINGLE_CHOICE -> {
                if (options.size() < 2 || correct != 1) {
                    throw invalid("Single-choice questions require at least two options and exactly one correct answer.");
                }
            }
            case MULTIPLE_CHOICE -> {
                if (options.size() < 2 || correct < 1) {
                    throw invalid("Multiple-choice questions require at least two options and at least one correct answer.");
                }
            }
            case TRUE_FALSE -> {
                if (options.size() != 2 || correct != 1) {
                    throw invalid("True/false questions require exactly two options and exactly one correct answer.");
                }
            }
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION_OPTIONS", message);
    }
}
