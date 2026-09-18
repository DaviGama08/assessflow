package com.davigama.assessflow.shared.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AssessmentNotFoundException extends DomainException {
    public AssessmentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "ASSESSMENT_NOT_FOUND", "Assessment " + id + " was not found");
    }
}
