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
        @NotBlank(message = "Target role cannot be null or blank")
        @Size(max = 100, message = "Target role must not exceed 100 characters")
        String targetRole,

        @Schema(description = "Bevorzugter Arbeitsort oder Remote-Präferenz", example = "Berlin (Remote möglich)")
        @NotBlank(message = "Location preference cannot be null or blank")
        @Size(max = 100, message = "Location preference must not exceed 100 characters")
        String locationPreference,

        @Schema(description = "Verfügbarkeit oder Kündigungsfrist", example = "Sofort")
        @NotBlank(message = "Availability cannot be null or blank")
        @Size(max = 100, message = "Availability must not exceed 100 characters")
        String availability,

        @Schema(description = "Erfahrungslevel (ENTRY, JUNIOR, MID, SENIOR, LEAD)", example = "SENIOR")
        @NotNull(message = "Experience level cannot be null")
        ExperienceLevel experienceLevel,

        @Schema(description = "Zusammenfassung des Profils / Kurzbiografie", example = "Erfahrener Java-Entwickler mit Schwerpunkt auf skalierbaren Microservices und Spring Boot.")
        @Size(max = 2000, message = "Summary must not exceed 2000 characters")
        String summary,

        @Schema(description = "Liste relevanter Fähigkeiten und Kenntnisse")
        @Size(max = 50, message = "Skills list must not exceed 50 items")
        List<@Valid ProfileSkillRequest> skills,

        @Schema(description = "Liste der Sprachkenntnisse")
        @Size(max = 10, message = "Languages list must not exceed 10 items")
        List<@Valid ProfileLanguageRequest> languages,

        @Schema(description = "Liste ausgewählter Referenzprojekte")
        @Size(max = 10, message = "Projects list must not exceed 10 items")
        List<@Valid ProfileProjectRequest> projects,

        @Schema(description = "Bevorzugte Rollenbezeichnungen", example = "[\"Senior Backend Developer\", \"Java Engineer\"]")
        @Size(max = 20, message = "Preferred roles must not exceed 20 items")
        Set<@NotBlank(message = "Preferred role cannot be blank") @Size(max = 100, message = "Preferred role must not exceed 100 characters") String> preferredRoles,

        @Schema(description = "Stichworte, die in Stellenangeboten vermieden werden sollen", example = "[\"PHP\", \"WordPress\"]")
        @Size(max = 20, message = "Avoid keywords must not exceed 20 items")
        Set<@NotBlank(message = "Avoid keyword cannot be blank") @Size(max = 100, message = "Avoid keyword must not exceed 100 characters") String> avoidKeywords
) {
}
