package com.melina.jobtrail.integration;

import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.profile.ProfileLanguageRequest;
import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.dto.profile.ProfileSkillRequest;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.ApplicationStatusHistory;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.ProfileRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.service.ApplicationService;
import com.melina.jobtrail.service.CompanyService;
import com.melina.jobtrail.service.ProfileService;
import com.melina.jobtrail.service.UserService;
import com.melina.jobtrail.util.ApplicationStatus;
import com.melina.jobtrail.util.ExperienceLevel;
import com.melina.jobtrail.util.LanguageLevel;
import com.melina.jobtrail.util.SkillLevel;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostgresPersistenceIntegrationTest extends AbstractPostgresCleanIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Test
    @DisplayName("Testcontainers PostgreSQL runs independently of local database with dynamic port")
    void postgresContainer_isRunningAndIsolatedFromLocalEnvironment() throws Exception {
        assertTrue(postgres.isCreated(), "PostgreSQL container must be created");
        assertTrue(postgres.isRunning(), "PostgreSQL container must be running");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            String url = metaData.getURL();

            assertEquals("PostgreSQL", dbProduct);
            assertTrue(dbVersion.contains("16"), "Database version must be PostgreSQL 16.x");
            assertTrue(url.startsWith("jdbc:postgresql://"), "JDBC URL must be PostgreSQL");
            assertNotEquals(5432, postgres.getMappedPort(5432),
                    "Port must be mapped dynamically, guaranteeing isolation from local PostgreSQL instances");
        }
    }

    @Test
    @DisplayName("Flyway migrations run on test start and bring schema to latest version")
    void flywayMigrations_appliedSuccessfullyOnTestStart() {
        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Current Flyway migration must not be null");
        assertEquals("4", current.getVersion().getVersion(), "Flyway schema must be at version 4");
        assertTrue(current.getState().isApplied(), "Latest Flyway migration must be in applied state");

        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertEquals(4, appliedMigrations.length, "All 4 migrations must be applied");

        List<String> scriptNames = Arrays.stream(appliedMigrations)
                .map(MigrationInfo::getScript)
                .toList();

        assertTrue(scriptNames.contains("V1__init.sql"));
        assertTrue(scriptNames.contains("V2__add_profiles.sql"));
        assertTrue(scriptNames.contains("V3__add_application_status_history.sql"));
        assertTrue(scriptNames.contains("V4__harden_application_queries.sql"));

        // Verify key tables exist in PostgreSQL information_schema
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("profiles"));
        assertTrue(tables.contains("companies"));
        assertTrue(tables.contains("applications"));
        assertTrue(tables.contains("application_status_history"));
        assertTrue(tables.contains("profile_skills"));
        assertTrue(tables.contains("profile_languages"));
        assertTrue(tables.contains("profile_projects"));
        assertTrue(tables.contains("profile_project_technologies"));
        assertTrue(tables.contains("profile_preferred_roles"));
        assertTrue(tables.contains("profile_avoid_keywords"));
    }

    @Test
    @DisplayName("Complete end-to-end service flow persists across User, Profile, Company, Application and StatusHistory in PostgreSQL")
    void completeServiceToDatabaseFlow_persistsAndVerifiesRealPostgresState() {
        String email = "flow-" + System.nanoTime() + "@jobtrail.dev";

        // 1. Service: User registration
        UserResponse registeredUser = userService.createUser(new RegisterRequest(email, "StrongPassword123!"));
        assertNotNull(registeredUser);
        assertEquals(email, registeredUser.email());

        // Verify User in PostgreSQL
        User userEntity = userRepository.findByEmail(email).orElseThrow();
        assertEquals(email, userEntity.getEmail());
        assertEquals(0L, userEntity.getVersion());
        assertNotNull(userEntity.getCreatedAt());

        // 2. Service: Profile creation with skills, languages, preferred roles
        ProfileRequest profileRequest = new ProfileRequest(
                "Senior Cloud Engineer",
                "Frankfurt (Hybrid)",
                "Immediately",
                ExperienceLevel.SENIOR,
                "Cloud infrastructure expert with Terraform and Kubernetes background.",
                List.of(
                        new ProfileSkillRequest("Kubernetes", SkillLevel.EXPERT, true),
                        new ProfileSkillRequest("Terraform", SkillLevel.PROFICIENT, false)
                ),
                List.of(new ProfileLanguageRequest("German", LanguageLevel.NATIVE)),
                List.of(),
                Set.of("Cloud Engineer", "DevOps Specialist"),
                Set.of("Legacy Support")
        );
        ProfileResponse createdProfile = profileService.createProfile(email, profileRequest);
        assertNotNull(createdProfile.id());
        assertEquals("Senior Cloud Engineer", createdProfile.targetRole());
        assertEquals(2, createdProfile.skills().size());

        // Verify Profile relationship and rows in PostgreSQL directly via JDBC
        Integer skillCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM profile_skills WHERE profile_id = ?",
                Integer.class,
                createdProfile.id()
        );
        assertEquals(2, skillCount);

        Integer languageCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM profile_languages WHERE profile_id = ?",
                Integer.class,
                createdProfile.id()
        );
        assertEquals(1, languageCount);

        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM profile_preferred_roles WHERE profile_id = ?",
                Integer.class,
                createdProfile.id()
        );
        assertEquals(2, roleCount);

        // 3. Service: Company creation
        CompanyResponse createdCompany = companyService.createCompany(email, new CompanyRequest(
                "CloudScale AG", "https://cloudscale.example.com", "Frankfurt am Main"
        ));
        assertNotNull(createdCompany.id());

        // Verify Company in PostgreSQL
        Company companyEntity = companyRepository.findById(createdCompany.id()).orElseThrow();
        assertEquals("CloudScale AG", companyEntity.getName());
        assertEquals(userEntity.getId(), companyEntity.getUser().getId());
        assertEquals(0L, companyEntity.getVersion());

        // 4. Service: Application creation
        ApplicationResponse createdApp = applicationService.createApplication(email, new ApplicationCreateRequest(
                "Lead Cloud Architect",
                createdCompany.id(),
                null,
                "https://cloudscale.example.com/jobs/123"
        ));
        assertNotNull(createdApp.id());
        assertEquals("Lead Cloud Architect", createdApp.positionTitle());
        assertEquals(ApplicationStatus.SAVED, createdApp.status());

        // Verify Application in PostgreSQL
        Application appEntity = applicationRepository.findById(createdApp.id()).orElseThrow();
        assertEquals(0L, appEntity.getVersion());
        assertEquals(companyEntity.getId(), appEntity.getCompany().getId());

        // Verify initial StatusHistory in PostgreSQL
        List<ApplicationStatusHistory> initialHistory =
                statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(createdApp.id());
        assertEquals(1, initialHistory.size());
        assertNull(initialHistory.getFirst().getPreviousStatus());
        assertEquals(ApplicationStatus.SAVED, initialHistory.getFirst().getNewStatus());

        // 5. Service: Status transition SAVED -> APPLIED
        ApplicationResponse updatedApp = applicationService.updateApplicationStatus(
                email,
                createdApp.id(),
                new ApplicationUpdateStatusRequest(ApplicationStatus.APPLIED)
        );
        assertEquals(ApplicationStatus.APPLIED, updatedApp.status());

        // Verify updated Application and version bump in PostgreSQL
        Application updatedAppEntity = applicationRepository.findById(createdApp.id()).orElseThrow();
        assertEquals(ApplicationStatus.APPLIED, updatedAppEntity.getStatus());
        assertEquals(1L, updatedAppEntity.getVersion(), "PostgreSQL version column must increment after status update");

        // Verify StatusHistory trail via Service and Database
        List<ApplicationStatusHistoryResponse> serviceHistory = applicationService.getStatusHistory(email, createdApp.id());
        assertEquals(2, serviceHistory.size());
        assertEquals(ApplicationStatus.SAVED, serviceHistory.get(1).previousStatus());
        assertEquals(ApplicationStatus.APPLIED, serviceHistory.get(1).newStatus());

        List<ApplicationStatusHistory> dbHistory =
                statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(createdApp.id());
        assertEquals(2, dbHistory.size());

        // 6. Cascade verification in PostgreSQL: delete Application -> status history cascade
        applicationRepository.delete(updatedAppEntity);
        applicationRepository.flush();

        assertTrue(applicationRepository.findById(createdApp.id()).isEmpty());
        assertTrue(statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(createdApp.id()).isEmpty(),
                "PostgreSQL ON DELETE CASCADE must remove all status history entries when application is deleted");
    }
}
