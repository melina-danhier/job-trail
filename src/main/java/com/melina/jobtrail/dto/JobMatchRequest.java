package com.melina.jobtrail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobMatchRequest (
        @NotBlank
        @Size(max = 12000)
        String description
) {
}
