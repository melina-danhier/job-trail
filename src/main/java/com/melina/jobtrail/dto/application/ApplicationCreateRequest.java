package com.melina.jobtrail.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Schema(description = "Anfragedaten zur Erstellung einer neuen Bewerbung")
public record ApplicationCreateRequest(

        @Schema(description = "Bezeichnung der angestrebten Position", example = "Senior Java Developer")
        @NotBlank(message = "Position title cannot be null or blank")
        @Size(max = 255, message = "Position title must not exceed 255 characters")
        String positionTitle,

        @Schema(description = "ID des zugeordneten Unternehmens", example = "1")
        @NotNull(message = "Company ID cannot be null")
        Long companyId,

        @Schema(description = "Datum der Bewerbung (Standard: aktuelles Datum)", example = "2026-10-17")
        LocalDate applicationDate,

        @Schema(description = "Link zur Online-Stellenausschreibung", example = "https://techcorp.example.com/jobs/senior-java-dev")
        @URL(message = "Invalid job URL format")
        @Size(max = 255, message = "Job URL must not exceed 255 characters")
        String jobUrl

) {
}
