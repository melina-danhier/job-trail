package com.melina.jobtrail.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Anfragedaten für ein Projekt im Profil")
public record ProfileProjectRequest(

        @Schema(description = "Titel des Projekts", example = "JobTrail REST API")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Beschreibung des Projekts", example = "Bewerbungsmanagement-System mit Spring Boot und PostgreSQL")
        @Size(max = 2000)
        String description,

        @Schema(description = "Im Projekt eingesetzte Technologien", example = "[\"Java\", \"Spring Boot\", \"Docker\"]")
        @Size(max = 30)
        Set<@NotBlank @Size(max = 100) String> technologies
) {
}
