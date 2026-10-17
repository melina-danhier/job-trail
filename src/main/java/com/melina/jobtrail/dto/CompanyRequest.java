package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Anfragedaten zur Erstellung oder Aktualisierung eines Unternehmens")
public record CompanyRequest(

        @Schema(description = "Name des Unternehmens", example = "TechCorp GmbH")
        @NotBlank(message = "Company name cannot be null or blank")
        @Size(max = 255, message = "Company name must not exceed 255 characters")
        String name,

        @Schema(description = "Offizielle Website des Unternehmens (muss gültige URL sein)", example = "https://techcorp.example.com")
        @URL(message = "Invalid website format")
        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website,

        @Schema(description = "Standort oder Hauptsitz des Unternehmens", example = "Berlin, Deutschland")
        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location

) {
}
