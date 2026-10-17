package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details einer Fähigkeit im Profil")
public record ProfileSkillResponse (
        @Schema(description = "Bezeichnung der Fähigkeit", example = "Spring Boot")
        String name,

        @Schema(description = "Kompetenzniveau", example = "EXPERT")
        SkillLevel level,

        @Schema(description = "Markierung als Kernkompetenz", example = "true")
        boolean mainSkill
) {
}
