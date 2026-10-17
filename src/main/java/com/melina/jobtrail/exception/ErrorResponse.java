package com.melina.jobtrail.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Getter
@JsonPropertyOrder({"timestamp", "status", "error", "message", "fieldErrors"})
@Schema(description = "Standardisierte Fehlerantwort der API")
public class ErrorResponse {

    @Schema(description = "Zeitpunkt des Fehlers im ISO-8601-Format", example = "2026-10-17T20:00:00Z")
    private final Instant timestamp;

    @Schema(description = "HTTP-Statuscode", example = "400")
    private final int status;

    @Schema(description = "HTTP-Statusbezeichnung", example = "Bad Request")
    private final String error;

    @Schema(description = "Detaillierte Fehlermeldung", example = "Invalid input data")
    private final String message;

    @Schema(description = "Feldspezifische Fehlermeldungen bei Validierungsfehlern (optional)", example = "{\"email\": \"Invalid email format\"}")
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
