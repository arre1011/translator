package org.trans.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex) {
        log.warn("Quota exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorResponse(
                        "QUOTA_EXCEEDED",
                        ex.getMessage(),
                        HttpStatus.PAYMENT_REQUIRED.value(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException ex) {
        log.warn("Session not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "SESSION_NOT_FOUND",
                        ex.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<ErrorResponse> handleOpenAiError(OpenAiException ex) {
        log.error("OpenAI error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        "OPENAI_ERROR",
                        "Failed to communicate with OpenAI: " + ex.getMessage(),
                        HttpStatus.BAD_GATEWAY.value(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        HttpStatus.BAD_REQUEST.value(),
                        Instant.now(),
                        errors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        Instant.now()
                ));
    }

    public record ErrorResponse(
            String code,
            String message,
            int status,
            Instant timestamp
    ) {
    }

    public record ValidationErrorResponse(
            String code,
            String message,
            int status,
            Instant timestamp,
            Map<String, String> errors
    ) {
    }
}
