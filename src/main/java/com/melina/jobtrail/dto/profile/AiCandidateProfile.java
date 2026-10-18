package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.ExperienceLevel;

import java.util.List;
import java.util.Set;

/**
 * Optimierter Datentransfer für den KI-Kontext, der rein fachliche Profildaten
 * ohne interne Datenbank-IDs oder Metadaten kapselt.
 */
public record AiCandidateProfile(
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
    public static AiCandidateProfile from(ProfileResponse profile) {
        if (profile == null) {
            return null;
        }
        return new AiCandidateProfile(
                profile.targetRole(),
                profile.locationPreference(),
                profile.availability(),
                profile.experienceLevel(),
                profile.summary(),
                profile.skills(),
                profile.languages(),
                profile.projects(),
                profile.preferredRoles(),
                profile.avoidKeywords()
        );
    }
}
