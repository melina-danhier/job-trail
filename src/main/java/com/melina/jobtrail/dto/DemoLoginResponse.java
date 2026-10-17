package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Antwort des 1-Klick Demo-Logins mit JWT-Token und Hinweisen für Tester")
public record DemoLoginResponse(
        @Schema(description = "JWT Bearer Token für den 'Authorize'-Button in Swagger UI", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Typ des Tokens", example = "Bearer")
        String tokenType,

        @Schema(description = "E-Mail-Adresse des Demo-Benutzers", example = "demo@jobtrail.dev")
        String email,

        @Schema(description = "Hinweis für Tester & Recruiter", example = "Demo-Login erfolgreich! Kopiere diesen Token und füge ihn oben rechts unter 'Authorize' ein.")
        String message
) {}
