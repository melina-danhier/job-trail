package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.JobMatchRequest;
import com.melina.jobtrail.dto.JobMatchResponse;
import com.melina.jobtrail.exception.ErrorResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.JobMatchAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-match")
@RequiredArgsConstructor
@Tag(name = "Job Match", description = "KI-gestützte Analyse und Matching von Stellenanzeigen mit dem Bewerberprofil")
public class JobMatchController {
    private final JobMatchAiService jobMatchAiService;

    @PostMapping("/analyze")
    @Operation(
            summary = "Stellenanzeige mit Profil abgleichen",
            description = "Führt eine KI-basierte Analyse durch, um eine übergebene Stellenbeschreibung mit dem hinterlegten Profil abzugleichen. Liefert einen Match-Score, übereinstimmende/fehlende Skills und Handlungsempfehlungen."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Analyse erfolgreich durchgeführt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JobMatchResponse.class),
                            examples = @ExampleObject(
                                    name = "AnalyseErgebnis",
                                    value = "{\"score\": 85, \"matchingSkills\": [\"Java\", \"Spring Boot\", \"REST APIs\", \"Docker\"], \"missingSkills\": [\"Kubernetes\"], \"recommendation\": \"Sehr gute Passung. Eine Bewerbung wird empfohlen.\", \"summary\": \"Das Profil deckt die Kernanforderungen für das Java-Backend exzellent ab.\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten (z. B. leere Stellenbeschreibung)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profil des Benutzers wurde nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate-Limit für KI-Analysen überschritten",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "RateLimitErreicht",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\", \"fieldErrors\": null}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "502", description = "Fehler bei der Kommunikation mit dem KI-Dienst oder ungültige Antwort",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "KI-Funktionalität ist serverseitig deaktiviert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<JobMatchResponse> analyze(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Zu analysierende Stellenbeschreibung",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JobMatchRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Stellenausschreibung",
                                    value = "{\"description\": \"Wir suchen einen Senior Java Backend Entwickler mit mindestens 5 Jahren Erfahrung in Java, Spring Boot, REST APIs und Docker. Kubernetes-Kenntnisse von Vorteil.\"}"
                            )
                    )
            )
            @Valid @RequestBody JobMatchRequest request
    ) {
        JobMatchResponse response = jobMatchAiService.analyze(userDetails.email(), request);
        return ResponseEntity.ok(response);
    }
}
