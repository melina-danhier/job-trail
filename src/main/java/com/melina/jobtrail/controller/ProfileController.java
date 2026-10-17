package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.exception.ErrorResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.ProfileService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Verwaltung des Bewerberprofils inkl. Skills, Sprachen und Projekten")
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping
    @Operation(summary = "Benutzerprofil erstellen", description = "Erstellt ein neues Bewerberprofil für den angemeldeten Benutzer.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Profil erfolgreich erstellt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponse.class),
                            examples = @ExampleObject(
                                    name = "ErstelltesProfil",
                                    value = "{\"id\": 1, \"targetRole\": \"Senior Backend Developer\", \"locationPreference\": \"Berlin (Remote möglich)\", \"availability\": \"Sofort\", \"experienceLevel\": \"SENIOR\", \"summary\": \"Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices.\", \"skills\": [{\"name\": \"Java\", \"level\": \"EXPERT\", \"mainSkill\": true}], \"languages\": [{\"language\": \"Deutsch\", \"level\": \"FLUENT\"}], \"projects\": [{\"name\": \"JobTrail REST API\", \"description\": \"Bewerbungsmanagement-System\", \"technologies\": [\"Java\", \"Spring Boot\"]}], \"preferredRoles\": [\"Senior Backend Developer\"], \"avoidKeywords\": [\"PHP\"]}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Profil existiert für diesen Benutzer bereits",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Profildaten des Bewerbers",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Profil",
                                    value = "{\"targetRole\": \"Senior Backend Developer\", \"locationPreference\": \"Berlin (Remote möglich)\", \"availability\": \"Sofort\", \"experienceLevel\": \"SENIOR\", \"summary\": \"Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices.\", \"skills\": [{\"name\": \"Java\", \"level\": \"EXPERT\", \"mainSkill\": true}], \"languages\": [{\"language\": \"Deutsch\", \"level\": \"FLUENT\"}], \"projects\": [{\"name\": \"JobTrail REST API\", \"description\": \"Bewerbungsmanagement-System\", \"technologies\": [\"Java\", \"Spring Boot\"]}], \"preferredRoles\": [\"Senior Backend Developer\"], \"avoidKeywords\": [\"PHP\"]}"
                            )
                    )
            )
            @Valid @RequestBody ProfileRequest request
    ) {
        ProfileResponse profile = profileService.createProfile(userDetails.email(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @GetMapping
    @Operation(summary = "Eigenes Profil abrufen", description = "Gibt das Profil des angemeldeten Benutzers zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponse.class),
                            examples = @ExampleObject(
                                    name = "EigenesProfil",
                                    value = "{\"id\": 1, \"targetRole\": \"Senior Backend Developer\", \"locationPreference\": \"Berlin (Remote möglich)\", \"availability\": \"Sofort\", \"experienceLevel\": \"SENIOR\", \"summary\": \"Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices.\", \"skills\": [{\"name\": \"Java\", \"level\": \"EXPERT\", \"mainSkill\": true}], \"languages\": [{\"language\": \"Deutsch\", \"level\": \"FLUENT\"}], \"projects\": [{\"name\": \"JobTrail REST API\", \"description\": \"Bewerbungsmanagement-System\", \"technologies\": [\"Java\", \"Spring Boot\"]}], \"preferredRoles\": [\"Senior Backend Developer\"], \"avoidKeywords\": [\"PHP\"]}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profil noch nicht angelegt",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails
    ) {
        ProfileResponse profile = profileService.getProfile(userDetails.email());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    @Operation(summary = "Eigenes Profil aktualisieren", description = "Aktualisiert das Profil des angemeldeten Benutzers vollständig.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil erfolgreich aktualisiert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponse.class),
                            examples = @ExampleObject(
                                    name = "AktualisiertesProfil",
                                    value = "{\"id\": 1, \"targetRole\": \"Lead Backend Architect\", \"locationPreference\": \"München (Remote)\", \"availability\": \"Ab 01.01.2027\", \"experienceLevel\": \"SENIOR\", \"summary\": \"Architekt für Cloud-native Java-Systeme.\", \"skills\": [{\"name\": \"Java\", \"level\": \"EXPERT\", \"mainSkill\": true}], \"languages\": [{\"language\": \"Deutsch\", \"level\": \"NATIVE\"}], \"projects\": [], \"preferredRoles\": [\"Lead Backend Architect\"], \"avoidKeywords\": []}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profil nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Aktualisierte Profildaten",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfileRequest.class),
                            examples = @ExampleObject(
                                    name = "ProfilAktualisierung",
                                    value = "{\"targetRole\": \"Lead Backend Architect\", \"locationPreference\": \"München (Remote)\", \"availability\": \"Ab 01.01.2027\", \"experienceLevel\": \"SENIOR\", \"summary\": \"Architekt für Cloud-native Java-Systeme.\", \"skills\": [{\"name\": \"Java\", \"level\": \"EXPERT\", \"mainSkill\": true}], \"languages\": [{\"language\": \"Deutsch\", \"level\": \"NATIVE\"}], \"projects\": [], \"preferredRoles\": [\"Lead Backend Architect\"], \"avoidKeywords\": []}"
                            )
                    )
            )
            @Valid @RequestBody ProfileRequest request
    ) {
        ProfileResponse profile = profileService.updateProfile(userDetails.email(), request);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping
    @Operation(summary = "Eigenes Profil löschen", description = "Löscht das Profil des angemeldeten Benutzers.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profil erfolgreich gelöscht"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profil nicht gefunden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails
    ) {
        profileService.deleteProfile(userDetails.email());
        return ResponseEntity.noContent().build();
    }
}
