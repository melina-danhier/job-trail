package com.melina.jobtrail.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Anfragedaten zur vollständigen Aktualisierung einer Bewerbung")
public record ApplicationUpdateRequest(

        @Schema(description = "Bezeichnung der angestrebten Position", example = "Lead Java Developer")
        @NotBlank(message = "Position title cannot be null or blank")
        @Size(max = 255, message = "Position title must not exceed 255 characters")
        String positionTitle,

        @Schema(description = "ID des zugeordneten Unternehmens", example = "1")
        @NotNull(message = "Company ID cannot be null")
        Long companyId,

        @Schema(description = "Datum der Bewerbung", example = "2026-10-17")
        LocalDate applicationDate,

        @Schema(description = "Link zur Online-Stellenausschreibung", example = "https://techcorp.example.com/jobs/lead-java-dev")
        @Size(max = 255, message = "Job URL must not exceed 255 characters")
        String jobUrl

) {
}
