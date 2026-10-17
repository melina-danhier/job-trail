package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ergebnis der KI-gestützten Profil- und Stellenanalyse")
public record JobMatchResponse (

        @Schema(description = "Übereinstimmungsscore zwischen Profil und Stelle (0-100)", example = "85")
        int score,

        @Schema(description = "Liste der vom Bewerber abgedeckten Fähigkeiten", example = "[\"Java\", \"Spring Boot\", \"REST APIs\", \"Docker\"]")
        List<String> matchingSkills,

        @Schema(description = "Liste der geforderten Fähigkeiten, die im Profil fehlen", example = "[\"Kubernetes\"]")
        List<String> missingSkills,

        @Schema(description = "Konkrete Handlungsempfehlung für den Bewerber", example = "Starke Übereinstimmung in den Kerntechnologien. Eine Bewerbung wird empfohlen.")
        String recommendation,

        @Schema(description = "Zusammenfassung der Bewertung", example = "Das Bewerberprofil passt hervorragend zu den Kernanforderungen der Stelle als Backend Entwickler.")
        String summary
) {
}
