package com.melina.jobtrail.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplicationUpdateRequest(
        @NotBlank(message = "Position title cannot be null or blank")
        @Size(max = 255, message = "Position title must not exceed 255 characters")
        String positionTitle,

        @NotNull(message = "Company ID cannot be null")
        Long companyId,

        LocalDate applicationDate,

        @Size(max = 255, message = "Job URL must not exceed 255 characters")
        String jobUrl
) {
}
