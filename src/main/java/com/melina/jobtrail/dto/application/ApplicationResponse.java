package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.util.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Details einer Bewerbung")
public record ApplicationResponse(

        @Schema(description = "Eindeutige ID der Bewerbung", example = "42")
        long id,

        @Schema(description = "Zugeordnetes Unternehmen")
        CompanyResponse company,

        @Schema(description = "Bezeichnung der Position", example = "Senior Java Developer")
        String positionTitle,

        @Schema(description = "Aktueller Status der Bewerbung", example = "APPLIED")
        ApplicationStatus status,

        @Schema(description = "Datum der Bewerbung", example = "2026-10-17")
        LocalDate applicationDate,

        @Schema(description = "Link zur Stellenanzeige", example = "https://techcorp.example.com/jobs/senior-java-dev")
        String jobUrl,

        @Schema(description = "Erstellungszeitpunkt", example = "2026-10-17T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Zeitpunkt der letzten Aktualisierung", example = "2026-10-17T11:30:00Z")
        Instant updatedAt

) {
}
