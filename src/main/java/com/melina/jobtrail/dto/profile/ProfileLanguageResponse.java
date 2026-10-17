package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.LanguageLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details einer Sprachkenntnis im Profil")
public record ProfileLanguageResponse (
        @Schema(description = "Bezeichnung der Sprache", example = "Deutsch")
        String language,

        @Schema(description = "Sprachniveau", example = "FLUENT")
        LanguageLevel level
) {
}
