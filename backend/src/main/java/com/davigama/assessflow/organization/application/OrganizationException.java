package com.davigama.assessflow.organization.application;

import org.springframework.http.HttpStatus;

public class OrganizationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public OrganizationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
