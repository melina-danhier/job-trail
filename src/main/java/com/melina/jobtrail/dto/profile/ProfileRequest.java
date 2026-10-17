package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.ExperienceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

@Schema(description = "Anfragedaten zur Erstellung oder Aktualisierung eines Bewerberprofils")
public record ProfileRequest(

        @Schema(description = "Angestrebte Rolle oder Position", example = "Senior Backend Developer")
        @NotBlank
        @Size(max = 100)
        String targetRole,

        @Schema(description = "Bevorzugter Arbeitsort oder Remote-Präferenz", example = "Berlin (Remote möglich)")
        @NotBlank
        @Size(max = 100)
        String locationPreference,

        @Schema(description = "Verfügbarkeit oder Kündigungsfrist", example = "Sofort")
        @NotBlank
        @Size(max = 100)
        String availability,

        @Schema(description = "Erfahrungslevel (ENTRY, JUNIOR, MID, SENIOR, LEAD)", example = "SENIOR")
        @NotNull
        ExperienceLevel experienceLevel,

        @Schema(description = "Zusammenfassung des Profils / Kurzbiografie", example = "Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices und Spring Boot.")
        @Size(max = 2000)
        String summary,

        @Schema(description = "Liste relevanter Fähigkeiten und Kenntnisse")
        @Size(max = 50)
        List<@Valid ProfileSkillRequest> skills,

        @Schema(description = "Liste der Sprachkenntnisse")
        @Size(max = 10)
        List<@Valid ProfileLanguageRequest> languages,

        @Schema(description = "Liste ausgewählter Referenzprojekte")
        @Size(max = 10)
        List<@Valid ProfileProjectRequest> projects,

        @Schema(description = "Bevorzugte Rollenbezeichnungen", example = "[\"Senior Backend Developer\", \"Java Engineer\"]")
        @Size(max = 20)
        Set<@NotBlank @Size(max = 100) String> preferredRoles,

        @Schema(description = "Stichworte, die in Stellenangeboten vermieden werden sollen", example = "[\"PHP\", \"WordPress\"]")
        @Size(max = 20)
        Set<@NotBlank @Size(max = 100) String> avoidKeywords
) {
}
