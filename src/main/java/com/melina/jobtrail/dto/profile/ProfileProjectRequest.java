package com.melina.jobtrail.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ProfileProjectRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 30)
        Set<@NotBlank @Size(max = 100) String> technologies
) {
}
