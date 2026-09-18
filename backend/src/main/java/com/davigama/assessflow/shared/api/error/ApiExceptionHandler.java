package com.davigama.assessflow.shared.api.error;

import com.davigama.assessflow.shared.exception.AssessmentNotFoundException;
import com.davigama.assessflow.identity.application.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ProblemDetail> auth(AuthException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "EMAIL_ALREADY_REGISTERED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return problem(status, status.getReasonPhrase(), exception.getMessage(), exception.getCode(), request);
    }
    @ExceptionHandler(AssessmentNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(AssessmentNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Assessment not found", exception.getMessage(),
                "ASSESSMENT_NOT_FOUND", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Check the request fields and try again.",
                "INVALID_REQUEST", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
                                                   String code, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setType(URI.create("https://httpstatuses.io/" + status.value()));
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("code", code);
        return ResponseEntity.status(status).body(body);
    }
}
