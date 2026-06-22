package com.melina.jobtrail.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Getter
@JsonPropertyOrder({"timestamp", "status", "error", "message", "fieldErrors"})
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(HttpStatus status, String message, Map<String, String> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public ErrorResponse(HttpStatus status, String message) {
        this(status, message, null);
    }
}
