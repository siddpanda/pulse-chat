package com.example.pulsechat.exception;

import com.example.pulsechat.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Single place where REST errors become consistent JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RoomNotFoundException ex, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
    }

    private ResponseEntity<ErrorResponse> body(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
