package com.davigama.assessflow.shared.api.error;

import com.davigama.assessflow.identity.application.AuthException;
import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException exception, HttpServletRequest request) {
        return problem(exception.getStatus(), exception.getStatus().getReasonPhrase(),
                exception.getMessage(), exception.getCode(), request);
    }

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ProblemDetail> auth(AuthException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "EMAIL_ALREADY_REGISTERED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return problem(status, status.getReasonPhrase(), exception.getMessage(), exception.getCode(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception exception, HttpServletRequest request) {
        if (isLocalPackageImport(request)) {
            return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The local event package is invalid.",
                    "INVALID_LOCAL_PACKAGE", request);
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Check the request fields and try again.",
                "INVALID_REQUEST", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> conflict(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", "The live session changed. Retry the command.",
                "LIVE_SESSION_CONFLICT", request);
    }

    private boolean isLocalPackageImport(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/local-live/packages");
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
