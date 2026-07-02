package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileSkillRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        SkillLevel level,

        boolean mainSkill
) {
}
