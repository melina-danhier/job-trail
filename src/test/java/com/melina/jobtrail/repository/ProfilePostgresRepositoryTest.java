package com.melina.jobtrail.repository;

import com.melina.jobtrail.AbstractPostgresIntegrationTest;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.entity.profile.Profile;
import com.melina.jobtrail.entity.profile.ProfileLanguage;
import com.melina.jobtrail.entity.profile.ProfileProject;
import com.melina.jobtrail.entity.profile.ProfileSkill;
import com.melina.jobtrail.util.ExperienceLevel;
import com.melina.jobtrail.util.LanguageLevel;
import com.melina.jobtrail.util.SkillLevel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProfilePostgresRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Persist complete Profile with skills, languages, projects, and collections in PostgreSQL")
    void saveAndFindProfile_withAllChildCollections_persistsSuccessfullyInPostgres() {
        Profile profile = Profile.builder()
                .targetRole("Lead Software Architect")
                .locationPreference("Remote / Munich")
                .availability("1 month notice")
                .experienceLevel(ExperienceLevel.SENIOR)
                .summary("Experienced engineer specializing in distributed systems and cloud native architectures.")
                .preferredRoles(new HashSet<>(Set.of("Lead Architect", "Principal Engineer")))
                .avoidKeywords(new HashSet<>(Set.of("PHP", "Legacy")))
                .build();

        ProfileSkill skillJava = ProfileSkill.builder()
                .name("Java 21")
                .level(SkillLevel.EXPERT)
                .mainSkill(true)
                .build();
        ProfileSkill skillPg = ProfileSkill.builder()
                .name("PostgreSQL")
                .level(SkillLevel.PROFICIENT)
                .mainSkill(false)
                .build();
        profile.setSkills(List.of(skillJava, skillPg));

        ProfileLanguage langDe = ProfileLanguage.builder()
                .language("German")
                .level(LanguageLevel.NATIVE)
                .build();
        ProfileLanguage langEn = ProfileLanguage.builder()
                .language("English")
                .level(LanguageLevel.FLUENT)
                .build();
        profile.setLanguages(List.of(langDe, langEn));

        ProfileProject project = ProfileProject.builder()
                .name("JobTrail Architecture")
                .description("Cloud-based job application platform")
                .technologies(new HashSet<>(Set.of("Spring Boot 4", "PostgreSQL 16", "Docker")))
                .build();
        profile.setProjects(List.of(project));

        Profile saved = profileRepository.save(profile);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(saved.getId());

        Profile reloaded = profileRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Lead Software Architect", reloaded.getTargetRole());
        assertEquals("Remote / Munich", reloaded.getLocationPreference());
        assertEquals(ExperienceLevel.SENIOR, reloaded.getExperienceLevel());
        assertEquals(2, reloaded.getSkills().size());
        assertEquals(2, reloaded.getLanguages().size());
        assertEquals(1, reloaded.getProjects().size());
        assertEquals(3, reloaded.getProjects().getFirst().getTechnologies().size());
        assertTrue(reloaded.getPreferredRoles().contains("Principal Engineer"));
        assertTrue(reloaded.getAvoidKeywords().contains("PHP"));
    }

    @Test
    @DisplayName("PostgreSQL enforces uc_users_profile unique constraint on profile_id")
    void linkingTwoUsersToSameProfile_violatesUniqueConstraintInPostgres() {
        Profile profile = Profile.builder()
                .targetRole("DevOps Engineer")
                .locationPreference("Remote")
                .availability("Immediate")
                .experienceLevel(ExperienceLevel.MID)
                .build();
        profileRepository.saveAndFlush(profile);

        User user1 = User.builder()
                .email("user1-profile@example.com")
                .passwordHash("hash1")
                .profile(profile)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .email("user2-profile@example.com")
                .passwordHash("hash2")
                .profile(profile)
                .build();

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });

        assertTrue(
                exception.getMessage().contains("uc_users_profile")
                        || (exception.getRootCause() != null && exception.getRootCause().getMessage().contains("uc_users_profile")),
                "Expected violation of uc_users_profile unique constraint"
        );
    }

    @Test
    @DisplayName("Deleting User cascades and removes Profile and child entities in PostgreSQL")
    void deletingUser_cascadesAndRemovesProfileAndChildrenInPostgres() {
        Profile profile = Profile.builder()
                .targetRole("Backend Engineer")
                .locationPreference("Berlin")
                .availability("3 months")
                .experienceLevel(ExperienceLevel.SENIOR)
                .build();
        profile.setSkills(List.of(ProfileSkill.builder().name("Go").level(SkillLevel.PROFICIENT).mainSkill(true).build()));

        User user = User.builder()
                .email("cascade-profile@example.com")
                .passwordHash("hash")
                .profile(profile)
                .build();

        userRepository.saveAndFlush(user);
        Long profileId = user.getProfile().getId();
        assertNotNull(profileId);

        entityManager.clear();

        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        userRepository.delete(reloadedUser);
        entityManager.flush();
        entityManager.clear();

        assertTrue(userRepository.findById(user.getId()).isEmpty());
        assertTrue(profileRepository.findById(profileId).isEmpty());
    }
}
