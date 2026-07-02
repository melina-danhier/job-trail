package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.ExperienceLevel;

import java.util.List;
import java.util.Set;

public record ProfileResponse (
        Long id,
        String targetRole,
        String locationPreference,
        String availability,
        ExperienceLevel experienceLevel,
        String summary,
        List<ProfileSkillResponse> skills,
        List<ProfileLanguageResponse> languages,
        List<ProfileProjectResponse> projects,
        Set<String> preferredRoles,
        Set<String> avoidKeywords
) {
}
