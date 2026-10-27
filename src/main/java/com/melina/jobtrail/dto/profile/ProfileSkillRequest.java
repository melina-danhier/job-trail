package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Anfragedaten für eine Fähigkeit im Benutzerprofil")
public record ProfileSkillRequest(

        @Schema(description = "Bezeichnung der Fähigkeit", example = "Spring Boot")
        @NotBlank(message = "Skill name cannot be null or blank")
        @Size(max = 100, message = "Skill name must not exceed 100 characters")
        String name,

        @Schema(description = "Kompetenzniveau (BEGINNER, ELEMENTARY, INTERMEDIATE, PROFICIENT, EXPERT)", example = "EXPERT")
        @NotNull(message = "Skill level cannot be null")
        SkillLevel level,

        @Schema(description = "Markierung als Kernkompetenz", example = "true")
        boolean mainSkill

) {
}
