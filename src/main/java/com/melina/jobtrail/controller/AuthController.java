package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.DemoLoginResponse;
import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.exception.ErrorResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.DemoDataService;
import com.melina.jobtrail.service.UserService;
import com.melina.jobtrail.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpunkte zur Registrierung, Authentifizierung und Benutzerverwaltung")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final DemoDataService demoDataService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            summary = "Neuen Benutzer registrieren",
            description = "Erstellt ein neues Benutzerkonto mit E-Mail und Passwort."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Benutzer erfolgreich registriert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "RegistrierterBenutzer",
                                    value = "{\"email\": \"new.applicant@jobtrail.dev\", \"createdAt\": \"2026-10-17T18:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültige Eingabedaten",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validierungsfehler",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validation failed\", \"fieldErrors\": {\"email\": \"Invalid email format\"}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-Mail-Adresse wird bereits verwendet",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "EmailBereitsVergeben",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Email already exists\", \"fieldErrors\": null}"
                            )
                    )
            )
    })
    public ResponseEntity<UserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registrierungsdaten für ein neues Benutzerkonto",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Registrierung",
                                    value = "{\"email\": \"new.applicant@jobtrail.dev\", \"password\": \"Secret123!\"}"
                            )
                    )
            )
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        UserResponse user = userService.createUser(registerRequest);
        demoDataService.initDemoData(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Benutzer anmelden",
            description = "Authentifiziert den Benutzer und gibt ein JWT-Bearer-Token zurück."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Erfolgreich angemeldet, JWT-Token zurückgegeben",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "Beispiel-JWT",
                                    value = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTczNDQ2NzIwMCwiZXhwIjoxNzM0NDcwODAwfQ.exampleSignature"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ungültige Eingabedaten",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "UngültigesFormat",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validation failed\", \"fieldErrors\": {\"email\": \"Invalid email format\"}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Ungültige Anmeldedaten",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FalscheAnmeldedaten",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Invalid credentials\", \"fieldErrors\": null}"
                            )
                    )
            )
    })
    public ResponseEntity<String> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Anmeldedaten des Benutzers",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Beispiel-Login",
                                    value = "{\"email\": \"demo@jobtrail.dev\", \"password\": \"JobTrail2026!\"}"
                            )
                    )
            )
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        if (DemoDataService.DEMO_EMAIL.equalsIgnoreCase(loginRequest.email())) {
            demoDataService.initDemoData(true);
        }
        String token = jwtUtil.generateToken(loginRequest.email());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/demo")
    @SecurityRequirements
    @Operation(
            summary = "1-Klick Demo-Login für Recruiter & Tester",
            description = "Erstellt oder lädt den vorbereiteten Demo-Account (demo@jobtrail.dev) mit Beispieldaten (Firmen, Bewerbungen, Profil) und gibt direkt einen gültigen JWT-Bearer-Token zurück. Standardmäßig werden alle Beispieldaten des Demo-Accounts vorab auf den sauberen Ausgangszustand zurückgesetzt."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Demo-Login erfolgreich, JWT-Token zurückgegeben",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DemoLoginResponse.class),
                            examples = @ExampleObject(
                                    name = "DemoLoginErfolgreich",
                                    value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.exampleSignature\", \"tokenType\": \"Bearer\", \"email\": \"demo@jobtrail.dev\", \"message\": \"Demo-Login erfolgreich! Kopiere diesen Token und füge ihn oben rechts unter 'Authorize' ein.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<DemoLoginResponse> demoLogin(
            @Parameter(description = "Falls true (Standard), werden die Beispieldaten des Demo-Accounts (Firmen, Bewerbungen, Profil) auf den Ausgangszustand zurückgesetzt.")
            @RequestParam(name = "reset", defaultValue = "true") boolean reset
    ) {
        DemoLoginResponse response = demoDataService.getOrCreateDemoToken(reset);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Aktuelle Benutzerinformationen abrufen", description = "Gibt die Profildaten des angemeldeten Benutzers zurück.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Benutzerdaten erfolgreich abgerufen",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "MeinBenutzerprofil",
                                    value = "{\"email\": \"demo@jobtrail.dev\", \"createdAt\": \"2026-10-17T18:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Nicht authentifiziert",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "NichtAuthentifiziert",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Full authentication is required to access this resource\", \"fieldErrors\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Benutzer nicht gefunden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "BenutzerNichtGefunden",
                                    value = "{\"timestamp\": \"2026-10-17T20:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"User not found\", \"fieldErrors\": null}"
                            )
                    )
            )
    })
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails userDetails) {
        UserResponse user = userService.getMe(userDetails.email());
        return ResponseEntity.ok(user);
    }
}
