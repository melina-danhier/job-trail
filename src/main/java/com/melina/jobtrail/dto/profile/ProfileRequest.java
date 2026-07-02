package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.ExperienceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record ProfileRequest(

        @NotBlank
        @Size(max = 100)
        String targetRole,

        @NotBlank
        @Size(max = 100)
        String locationPreference,

        @NotBlank
        @Size(max = 100)
        String availability,

        @NotNull
        ExperienceLevel experienceLevel,

        @Size(max = 2000)
        String summary,

        @Size(max = 50)
        List<@Valid ProfileSkillRequest> skills,

        @Size(max = 10)
        List<@Valid ProfileLanguageRequest> languages,

        @Size(max = 10)
        List<@Valid ProfileProjectRequest> projects,

        @Size(max = 20)
        Set<@NotBlank @Size(max = 100) String> preferredRoles,

        @Size(max = 20)
        Set<@NotBlank @Size(max = 100) String> avoidKeywords
) {
}
