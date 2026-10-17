package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.DemoLoginResponse;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.ApplicationStatusHistory;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.entity.profile.Profile;
import com.melina.jobtrail.entity.profile.ProfileLanguage;
import com.melina.jobtrail.entity.profile.ProfileProject;
import com.melina.jobtrail.entity.profile.ProfileSkill;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import com.melina.jobtrail.util.ExperienceLevel;
import com.melina.jobtrail.util.JwtUtil;
import com.melina.jobtrail.util.LanguageLevel;
import com.melina.jobtrail.util.SkillLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoDataService {

    public static final String DEMO_EMAIL = "demo@jobtrail.dev";
    public static final String DEMO_PASSWORD = "JobTrail2026!";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public DemoLoginResponse getOrCreateDemoToken(boolean reset) {
        if (reset || !userRepository.existsByEmail(DEMO_EMAIL)) {
            initDemoData(reset);
        }
        String token = jwtUtil.generateToken(DEMO_EMAIL);
        return new DemoLoginResponse(
                token,
                "Bearer",
                DEMO_EMAIL,
                "Demo-Login erfolgreich! Kopiere diesen Token und füge ihn oben rechts unter 'Authorize' ein (Format: 'Bearer <token>' oder nur '<token>')."
        );
    }

    @Transactional
    public void initDemoData(boolean forceReset) {
        Optional<User> existingUserOpt = userRepository.findByEmail(DEMO_EMAIL);

        if (existingUserOpt.isPresent() && !forceReset) {
            log.info("Demo user '{}' already exists. Skipping initialization.", DEMO_EMAIL);
            return;
        }

        User user;
        if (existingUserOpt.isPresent()) {
            log.info("Resetting demo data for user '{}'...", DEMO_EMAIL);
            user = existingUserOpt.get();
            // Clean up existing applications & status history
            List<Application> apps = applicationRepository.findAllByUserId(user.getId());
            for (Application app : apps) {
                statusHistoryRepository.deleteAll(statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(app.getId()));
            }
            applicationRepository.deleteAll(apps);
            // Clean up existing companies
            companyRepository.deleteAll(companyRepository.findAllByUserId(user.getId()));
            // Clear profile
            if (user.getProfile() != null) {
                user.setProfile(null);
                user = userRepository.saveAndFlush(user);
            }
        } else {
            log.info("Creating fresh demo user '{}'...", DEMO_EMAIL);
            user = User.builder()
                    .email(DEMO_EMAIL)
                    .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                    .build();
            user = userRepository.save(user);
        }

        // 1. Seed Profile
        Profile profile = Profile.builder()
                .targetRole("Senior Java & Cloud Software Engineer")
                .locationPreference("Berlin / Remote")
                .availability("Sofort (1 Monat Kündigungsfrist)")
                .experienceLevel(ExperienceLevel.SENIOR)
                .summary("Leidenschaftlicher Senior Softwareentwickler mit fundierter Erfahrung in Spring Boot, REST APIs, Microservice-Architekturen, PostgreSQL und Cloud-Deployments.")
                .preferredRoles(new HashSet<>(List.of("Senior Backend Engineer", "Lead Java Developer", "Cloud Application Engineer")))
                .avoidKeywords(new HashSet<>(List.of("PHP", "Wordpress", "Legacy Monolith", "100% Reisetätigkeit")))
                .build();

        profile.setSkills(List.of(
                ProfileSkill.builder().name("Java").level(SkillLevel.EXPERT).mainSkill(true).build(),
                ProfileSkill.builder().name("Spring Boot").level(SkillLevel.EXPERT).mainSkill(true).build(),
                ProfileSkill.builder().name("PostgreSQL").level(SkillLevel.PROFICIENT).mainSkill(true).build(),
                ProfileSkill.builder().name("Docker & Kubernetes").level(SkillLevel.PROFICIENT).mainSkill(false).build(),
                ProfileSkill.builder().name("AWS / Cloud").level(SkillLevel.INTERMEDIATE).mainSkill(false).build(),
                ProfileSkill.builder().name("REST APIs & OpenAPI").level(SkillLevel.EXPERT).mainSkill(true).build()
        ));

        profile.setLanguages(List.of(
                ProfileLanguage.builder().language("Deutsch").level(LanguageLevel.NATIVE).build(),
                ProfileLanguage.builder().language("Englisch").level(LanguageLevel.FLUENT).build()
        ));

        ProfileProject project1 = ProfileProject.builder()
                .name("JobTrail Application Tracker")
                .description("Spring Boot 4 Backend mit OpenAPI/Swagger-Dokumentation, Flyway-Migrationen und Spring AI Stellenanzeigen-Matching.")
                .technologies(new HashSet<>(List.of("Java 21", "Spring Boot", "Spring AI", "PostgreSQL", "OpenAPI", "Docker")))
                .build();

        ProfileProject project2 = ProfileProject.builder()
                .name("Enterprise Microservice Hub")
                .description("Entwicklung einer skalierbaren Event-Driven Microservice-Architektur mit Apache Kafka und Spring Cloud.")
                .technologies(new HashSet<>(List.of("Java", "Spring Boot", "Kafka", "Kubernetes", "Redis")))
                .build();

        profile.setProjects(List.of(project1, project2));

        user.setProfile(profile);
        user = userRepository.save(user);

        // 2. Seed Companies
        Company spotify = companyRepository.save(Company.builder()
                .user(user)
                .name("Spotify")
                .website("https://spotify.com")
                .location("Stockholm / Remote")
                .build());

        Company sap = companyRepository.save(Company.builder()
                .user(user)
                .name("SAP")
                .website("https://sap.com")
                .location("Walldorf / Hybrid")
                .build());

        Company zalando = companyRepository.save(Company.builder()
                .user(user)
                .name("Zalando")
                .website("https://zalando.de")
                .location("Berlin")
                .build());

        // 3. Seed Applications & Status History
        Instant now = Instant.now();

        // Application 1: Spotify
        Application appSpotify = applicationRepository.save(Application.builder()
                .user(user)
                .company(spotify)
                .positionTitle("Senior Java Backend Engineer")
                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                .applicationDate(LocalDate.now().minusDays(14))
                .jobUrl("https://jobs.spotify.com/senior-java-backend-engineer")
                .build());

        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appSpotify)
                .previousStatus(null)
                .newStatus(ApplicationStatus.APPLIED)
                .changedAt(now.minus(Duration.ofDays(14)))
                .build());
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appSpotify)
                .previousStatus(ApplicationStatus.APPLIED)
                .newStatus(ApplicationStatus.UNDER_REVIEW)
                .changedAt(now.minus(Duration.ofDays(7)))
                .build());
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appSpotify)
                .previousStatus(ApplicationStatus.UNDER_REVIEW)
                .newStatus(ApplicationStatus.INTERVIEW_SCHEDULED)
                .changedAt(now.minus(Duration.ofDays(2)))
                .build());

        // Application 2: SAP
        Application appSap = applicationRepository.save(Application.builder()
                .user(user)
                .company(sap)
                .positionTitle("Fullstack Developer (Java / React)")
                .status(ApplicationStatus.APPLIED)
                .applicationDate(LocalDate.now().minusDays(5))
                .jobUrl("https://jobs.sap.com/fullstack-developer")
                .build());

        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appSap)
                .previousStatus(null)
                .newStatus(ApplicationStatus.APPLIED)
                .changedAt(now.minus(Duration.ofDays(5)))
                .build());

        // Application 3: Zalando
        Application appZalando = applicationRepository.save(Application.builder()
                .user(user)
                .company(zalando)
                .positionTitle("Software Engineer - Platform & Cloud")
                .status(ApplicationStatus.OFFER_RECEIVED)
                .applicationDate(LocalDate.now().minusDays(28))
                .jobUrl("https://jobs.zalando.com/platform-engineer")
                .build());

        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appZalando)
                .previousStatus(null)
                .newStatus(ApplicationStatus.APPLIED)
                .changedAt(now.minus(Duration.ofDays(28)))
                .build());
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appZalando)
                .previousStatus(ApplicationStatus.APPLIED)
                .newStatus(ApplicationStatus.INTERVIEW_COMPLETED)
                .changedAt(now.minus(Duration.ofDays(10)))
                .build());
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(appZalando)
                .previousStatus(ApplicationStatus.INTERVIEW_COMPLETED)
                .newStatus(ApplicationStatus.OFFER_RECEIVED)
                .changedAt(now.minus(Duration.ofDays(3)))
                .build());

        log.info("Demo data seeding completed successfully for user '{}'.", DEMO_EMAIL);
    }
}
