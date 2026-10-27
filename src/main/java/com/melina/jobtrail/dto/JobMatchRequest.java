package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Anfragedaten zur KI-gestützten Analyse eines Jobangebots gegen das Benutzerprofil")
public record JobMatchRequest (

        @Schema(
                description = "Vollständiger Text oder Auszug der Stellenanzeige",
                example = "Wir suchen einen Senior Java Backend Entwickler mit mindestens 5 Jahren Erfahrung in Java, Spring Boot, REST APIs und Docker. Kubernetes-Kenntnisse von Vorteil."
        )
        @NotBlank(message = "Job description cannot be null or blank")
        @Size(max = 12000, message = "Job description must not exceed 12000 characters")
        String description
) {
}
