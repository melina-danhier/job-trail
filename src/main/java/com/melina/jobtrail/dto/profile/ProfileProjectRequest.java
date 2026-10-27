package com.melina.jobtrail.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Anfragedaten für ein Projekt im Profil")
public record ProfileProjectRequest(

        @Schema(description = "Titel des Projekts", example = "JobTrail REST API")
        @NotBlank(message = "Project name cannot be null or blank")
        @Size(max = 100, message = "Project name must not exceed 100 characters")
        String name,

        @Schema(description = "Beschreibung des Projekts", example = "Bewerbungsmanagement-System mit Spring Boot und PostgreSQL")
        @Size(max = 2000, message = "Project description must not exceed 2000 characters")
        String description,

        @Schema(description = "Im Projekt eingesetzte Technologien", example = "[\"Java\", \"Spring Boot\", \"Docker\"]")
        @Size(max = 30, message = "Technologies must not exceed 30 items")
        Set<@NotBlank(message = "Technology cannot be blank") @Size(max = 100, message = "Technology must not exceed 100 characters") String> technologies
) {
}
