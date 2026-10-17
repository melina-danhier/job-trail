package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Informationen über den authentifizierten Benutzer")
public record UserResponse(

        @Schema(description = "E-Mail-Adresse des Benutzers", example = "john.doe@example.com")
        String email,

        @Schema(description = "Zeitpunkt der Registrierung", example = "2026-10-17T18:00:00Z")
        Instant createdAt

) {
}
