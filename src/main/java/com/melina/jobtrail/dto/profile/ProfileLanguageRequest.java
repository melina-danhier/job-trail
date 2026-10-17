package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.LanguageLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Anfragedaten für eine Sprachkenntnis im Profil")
public record ProfileLanguageRequest(

        @Schema(description = "Bezeichnung der Sprache", example = "Deutsch")
        @NotBlank
        @Size(max = 50)
        String language,

        @Schema(description = "Sprachniveau (BASIC, INTERMEDIATE, ADVANCED, FLUENT, NATIVE)", example = "FLUENT")
        @NotNull
        LanguageLevel level
) {
}
