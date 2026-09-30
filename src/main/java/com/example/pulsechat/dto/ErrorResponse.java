package com.example.pulsechat.dto;

import java.time.Instant;

/**
 * Consistent error body returned by the REST API and STOMP error handler.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
