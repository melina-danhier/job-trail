package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Anfragedaten für eine Fähigkeit im Benutzerprofil")
public record ProfileSkillRequest(

        @Schema(description = "Bezeichnung der Fähigkeit", example = "Spring Boot")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Kompetenzniveau (BEGINNER, ELEMENTARY, INTERMEDIATE, PROFICIENT, EXPERT)", example = "EXPERT")
        @NotNull
        SkillLevel level,

        @Schema(description = "Markierung als Kernkompetenz", example = "true")
        boolean mainSkill

) {
}
