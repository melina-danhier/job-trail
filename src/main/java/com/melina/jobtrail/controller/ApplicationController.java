package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.exception.ErrorResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.ApplicationService;
import com.melina.jobtrail.util.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Applications", description = "Verwaltung von Bewerbungen, Status-Tracking und Historie")
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Neue Bewerbung anlegen", description = "Erstellt eine neue Bewerbung für den angemeldeten Benutzer.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Bewerbung erfolgreich angelegt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationResponse.class),
                            examples = @ExampleObject(
                                    name = "ErstellteBewerbung",
                                    value = "{\"id\": 42, \"company\": {\"id\": 1, \"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}, \"positionTitle\": \"Senior Java Developer\", \"status\": \"APPLIED\", \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/senior-java-dev\", \"createdAt\": \"2026-10-17T10:00:00Z\", \"updatedAt\": \"2026-10-17T10:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unternehmen nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Bewerbung für diese Position und dieses Unternehmen existiert bereits",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApplicationResponse> createApplication(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Daten zur Erstellung einer neuen Bewerbung",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Bewerbung",
                                    value = "{\"positionTitle\": \"Cloud DevOps Engineer\", \"companyId\": 4, \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/cloud-devops\"}"
                            )
                    )
            )
            @Valid @RequestBody ApplicationCreateRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.createApplication(userDetails.email(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Operation(summary = "Bewerbungen abrufen", description = "Ruft Bewerbungen paginiert, sortiert und optional nach Status, Unternehmen oder Zeitraum gefiltert ab.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bewerbungen erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(
                                    name = "PaginierteBewerbungen",
                                    value = "{\"content\": [{\"id\": 42, \"company\": {\"id\": 1, \"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}, \"positionTitle\": \"Senior Java Developer\", \"status\": \"APPLIED\", \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/senior-java-dev\", \"createdAt\": \"2026-10-17T10:00:00Z\", \"updatedAt\": \"2026-10-17T10:00:00Z\"}], \"page\": 0, \"size\": 20, \"totalElements\": 1, \"totalPages\": 1, \"first\": true, \"last\": true}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Paginierungs- oder Filterparameter",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "Seitennummer (0-basiert)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Anzahl der Einträge pro Seite (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Feld für die Sortierung", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sortierrichtung (ASC oder DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "Optionaler Filter nach Bewerbungsstatus (z. B. APPLIED, INTERVIEW_SCHEDULED, OFFER_RECEIVED)")
            @RequestParam(required = false) ApplicationStatus status,
            @Parameter(description = "Optionaler Filter nach Unternehmens-ID")
            @RequestParam(required = false) Long companyId,
            @Parameter(description = "Optionaler Filter: Bewerbungsdatum von (ISO-Format YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDateFrom,
            @Parameter(description = "Optionaler Filter: Bewerbungsdatum bis (ISO-Format YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDateTo
    ) {
        return ResponseEntity.ok(applicationService.getApplications(
                userDetails.email(), page, size, sortBy, direction,
                status, companyId, applicationDateFrom, applicationDateTo
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Bewerbung nach ID abrufen", description = "Gibt die Details einer bestimmten Bewerbung zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bewerbung gefunden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationResponse.class),
                            examples = @ExampleObject(
                                    name = "GefundeneBewerbung",
                                    value = "{\"id\": 42, \"company\": {\"id\": 1, \"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}, \"positionTitle\": \"Senior Java Developer\", \"status\": \"APPLIED\", \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/senior-java-dev\", \"createdAt\": \"2026-10-17T10:00:00Z\", \"updatedAt\": \"2026-10-17T10:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bewerbung nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID der Bewerbung", example = "4") @PathVariable long id
    ) {
        ApplicationResponse dto = applicationService.getApplicationById(userDetails.email(), id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Bewerbung aktualisieren", description = "Aktualisiert die Daten einer bestehenden Bewerbung.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bewerbung erfolgreich aktualisiert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationResponse.class),
                            examples = @ExampleObject(
                                    name = "AktualisierteBewerbung",
                                    value = "{\"id\": 42, \"company\": {\"id\": 1, \"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}, \"positionTitle\": \"Lead Java Developer\", \"status\": \"APPLIED\", \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/lead-java-dev\", \"createdAt\": \"2026-10-17T10:00:00Z\", \"updatedAt\": \"2026-10-17T12:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bewerbung oder Unternehmen nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApplicationResponse> updateApplication(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID der Bewerbung", example = "4") @PathVariable long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Aktualisierte Bewerbungsdaten",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Aktualisierung",
                                    value = "{\"positionTitle\": \"Lead Java Developer\", \"companyId\": 4, \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://techcorp.example.com/jobs/lead-java-dev\"}"
                            )
                    )
            )
            @Valid @RequestBody ApplicationUpdateRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.updateApplication(userDetails.email(), id, requestDto);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Bewerbungsstatus aktualisieren", description = "Ändert den Status einer Bewerbung und protokolliert die Statusänderung in der Historie.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status erfolgreich geändert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationResponse.class),
                            examples = @ExampleObject(
                                    name = "StatusGeaendert",
                                    value = "{\"id\": 4, \"company\": {\"id\": 4, \"name\": \"Spotify\", \"website\": \"https://spotify.com\", \"location\": \"Stockholm / Remote\"}, \"positionTitle\": \"Senior Java Backend Engineer\", \"status\": \"INTERVIEW_SCHEDULED\", \"applicationDate\": \"2026-10-17\", \"jobUrl\": \"https://jobs.spotify.com/senior-java-backend-engineer\", \"createdAt\": \"2026-10-17T10:00:00Z\", \"updatedAt\": \"2026-10-17T14:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültiger Status",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bewerbung nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID der Bewerbung", example = "4") @PathVariable long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Neuer Status der Bewerbung",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationUpdateStatusRequest.class),
                            examples = @ExampleObject(
                                    name = "StatusWechsel",
                                    value = "{\"status\": \"INTERVIEW_COMPLETED\"}"
                            )
                    )
            )
            @Valid @RequestBody ApplicationUpdateStatusRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.updateApplicationStatus(userDetails.email(), id, requestDto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/status-history")
    @Operation(summary = "Statusverlauf abrufen", description = "Gibt die chronologische Historie aller Statuswechsel dieser Bewerbung zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statusverlauf erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ApplicationStatusHistoryResponse.class)),
                            examples = @ExampleObject(
                                    name = "Statushistorie",
                                    value = "[{\"id\": 101, \"previousStatus\": \"APPLIED\", \"newStatus\": \"INTERVIEW_SCHEDULED\", \"changedAt\": \"2026-10-17T14:00:00Z\"}]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bewerbung nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ApplicationStatusHistoryResponse>> getStatusHistory(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID der Bewerbung", example = "4") @PathVariable long id
    ) {
        return ResponseEntity.ok(applicationService.getStatusHistory(userDetails.email(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Bewerbung löschen", description = "Entfernt eine Bewerbung mitsamt ihrer Statushistorie.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bewerbung erfolgreich gelöscht"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bewerbung nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID der Bewerbung", example = "4") @PathVariable long id
    ) {
        applicationService.deleteApplication(userDetails.email(), id);
        return ResponseEntity.noContent().build();
    }
}
