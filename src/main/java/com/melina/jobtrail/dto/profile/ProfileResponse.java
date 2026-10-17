package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.ExperienceLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

@Schema(description = "Details des Benutzerprofils")
public record ProfileResponse (

        @Schema(description = "Eindeutige ID des Profils", example = "1")
        Long id,

        @Schema(description = "Angestrebte Rolle oder Position", example = "Senior Backend Developer")
        String targetRole,

        @Schema(description = "Bevorzugter Arbeitsort oder Remote-Präferenz", example = "Berlin (Remote möglich)")
        String locationPreference,

        @Schema(description = "Verfügbarkeit oder Kündigungsfrist", example = "Sofort")
        String availability,

        @Schema(description = "Erfahrungslevel", example = "SENIOR")
        ExperienceLevel experienceLevel,

        @Schema(description = "Zusammenfassung des Profils", example = "Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices und Spring Boot.")
        String summary,

        @Schema(description = "Liste relevanter Fähigkeiten und Kenntnisse")
        List<ProfileSkillResponse> skills,

        @Schema(description = "Liste der Sprachkenntnisse")
        List<ProfileLanguageResponse> languages,

        @Schema(description = "Liste ausgewählter Referenzprojekte")
        List<ProfileProjectResponse> projects,

        @Schema(description = "Bevorzugte Rollenbezeichnungen", example = "[\"Senior Backend Developer\", \"Java Engineer\"]")
        Set<String> preferredRoles,

        @Schema(description = "Stichworte, die in Stellenangeboten vermieden werden sollen", example = "[\"PHP\", \"WordPress\"]")
        Set<String> avoidKeywords
) {
}
