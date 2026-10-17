package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unternehmensdaten")
public record CompanyResponse(

        @Schema(description = "Eindeutige ID des Unternehmens", example = "1")
        long id,

        @Schema(description = "Name des Unternehmens", example = "TechCorp GmbH")
        String name,

        @Schema(description = "Offizielle Website des Unternehmens", example = "https://techcorp.example.com")
        String website,

        @Schema(description = "Standort des Unternehmens", example = "Berlin, Deutschland")
        String location

) {
}
