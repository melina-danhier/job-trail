package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Anfragedaten zur Registrierung eines neuen Benutzers")
public record RegisterRequest(

        @Schema(description = "E-Mail-Adresse des neuen Benutzers", example = "new.applicant@jobtrail.dev")
        @NotBlank(message = "Email cannot be null or blank")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Passwort (mindestens 8 Zeichen)", example = "Secret123!")
        @NotBlank(message = "Password cannot be null or blank")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        String password

) {
}
