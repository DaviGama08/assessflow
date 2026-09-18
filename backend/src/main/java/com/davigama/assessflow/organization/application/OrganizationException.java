package com.davigama.assessflow.organization.application;

import com.davigama.assessflow.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class OrganizationException extends DomainException {
    public OrganizationException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
