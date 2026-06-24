package com.melina.jobtrail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record RequestCompanyDto(
        @NotBlank(message = "Company name cannot be null or blank")
        @Size(max = 255, message = "Company name must not exceed 255 characters")
        String name,

        @URL(message = "Invalid website format")
        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location
) {
}
