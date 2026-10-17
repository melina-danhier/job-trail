package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.exception.ErrorResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.CompanyService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Verwaltung von Unternehmen und potenziellen Arbeitgebern")
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    @Operation(summary = "Neues Unternehmen anlegen", description = "Erstellt ein neues Unternehmen für den angemeldeten Benutzer.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Unternehmen erfolgreich erstellt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyResponse.class),
                            examples = @ExampleObject(
                                    name = "ErstelltesUnternehmen",
                                    value = "{\"id\": 1, \"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültige Eingabedaten (z. B. fehlender Name oder ungültige URL)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "UngueltigeURL",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validation failed\", \"fieldErrors\": {\"website\": \"Invalid website format\"}}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompanyResponse> createCompany(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Daten des neuen Unternehmens",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Unternehmen",
                                    value = "{\"name\": \"TechCorp GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"Berlin, Deutschland\"}"
                            )
                    )
            )
            @Valid @RequestBody CompanyRequest requestDto
    ) {
        CompanyResponse dto = companyService.createCompany(userDetails.email(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Operation(summary = "Alle Unternehmen abrufen", description = "Gibt die Liste aller Unternehmen des angemeldeten Benutzers zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste der Unternehmen erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CompanyResponse.class)),
                            examples = @ExampleObject(
                                    name = "Unternehmensliste",
                                    value = "[{\"id\": 4, \"name\": \"Spotify\", \"website\": \"https://spotify.com\", \"location\": \"Stockholm / Remote\"}]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CompanyResponse>> getAllCompanies(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails
    ) {
        List<CompanyResponse> dto = companyService.getAllCompanies(userDetails.email());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Unternehmen anhand ID abrufen", description = "Gibt ein bestimmtes Unternehmen des Benutzers anhand seiner ID zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Unternehmen gefunden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyResponse.class),
                            examples = @ExampleObject(
                                    name = "GefundenesUnternehmen",
                                    value = "{\"id\": 4, \"name\": \"Spotify\", \"website\": \"https://spotify.com\", \"location\": \"Stockholm / Remote\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unternehmen nicht gefunden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "NichtGefunden",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Company not found\", \"fieldErrors\": null}"
                            )
                    )
            )
    })
    public ResponseEntity<CompanyResponse> getCompanyById(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID des Unternehmens", example = "4") @PathVariable Long id
    ) {
        CompanyResponse dto = companyService.getCompanyById(userDetails.email(), id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Unternehmen aktualisieren", description = "Aktualisiert die Daten eines bestehenden Unternehmens.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Unternehmen erfolgreich aktualisiert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyResponse.class),
                            examples = @ExampleObject(
                                     name = "AktualisiertesUnternehmen",
                                     value = "{\"id\": 4, \"name\": \"TechCorp International GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"München, Deutschland\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unternehmen nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompanyResponse> updateCompany(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID des Unternehmens", example = "4") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Aktualisierte Unternehmensdaten",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyRequest.class),
                            examples = @ExampleObject(
                                    name = "Aktualisierungsbeispiel",
                                    value = "{\"name\": \"TechCorp International GmbH\", \"website\": \"https://techcorp.example.com\", \"location\": \"München, Deutschland\"}"
                            )
                    )
            )
            @Valid @RequestBody CompanyRequest requestDto
    ) {
        CompanyResponse dto = companyService.updateCompany(userDetails.email(), id, requestDto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Unternehmen löschen", description = "Löscht ein Unternehmen, sofern ihm keine aktiven Bewerbungen mehr zugeordnet sind.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Unternehmen erfolgreich gelöscht"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unternehmen nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Unternehmen kann nicht gelöscht werden, da ihm Bewerbungen zugeordnet sind",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "KonfliktBewerbungenVorhanden",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Cannot delete company with active applications\", \"fieldErrors\": null}"
                            )
                    )
            )
    })
    public ResponseEntity<Void> deleteCompany(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "ID des Unternehmens", example = "4") @PathVariable Long id
    ) {
        companyService.deleteCompany(userDetails.email(), id);
        return ResponseEntity.noContent().build();
    }
}
