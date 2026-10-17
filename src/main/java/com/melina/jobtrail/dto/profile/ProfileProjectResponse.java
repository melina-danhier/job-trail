package com.melina.jobtrail.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Details eines Projekts im Profil")
public record ProfileProjectResponse (
        @Schema(description = "Titel des Projekts", example = "JobTrail REST API")
        String name,

        @Schema(description = "Beschreibung des Projekts", example = "Bewerbungsmanagement-System mit Spring Boot und PostgreSQL")
        String description,

        @Schema(description = "Im Projekt eingesetzte Technologien", example = "[\"Java\", \"Spring Boot\", \"Docker\"]")
        Set<String> technologies
) {
}
