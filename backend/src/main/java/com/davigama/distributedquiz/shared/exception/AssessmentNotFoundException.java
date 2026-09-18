package com.davigama.distributedquiz.shared.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(UUID id) {
        super("Assessment " + id + " was not found");
    }
}
