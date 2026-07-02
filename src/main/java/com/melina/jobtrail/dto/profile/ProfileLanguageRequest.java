package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.LanguageLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileLanguageRequest(

        @NotBlank
        @Size(max = 50)
        String language,

        @NotNull
        LanguageLevel level
) {
}
